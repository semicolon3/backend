package com.legalai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.domain.Conversation;
import com.legalai.domain.LegalDomain;
import com.legalai.domain.Message;
import com.legalai.domain.MessageRole;
import com.legalai.dto.request.SendMessageRequest;
import com.legalai.dto.response.MessageResponse;
import com.legalai.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RAG 기반 법률 챗봇 핵심 서비스.
 * <p>
 * 흐름:
 * <pre>
 * 사용자 질문
 *   ↓ (1) 이전 대화 이력 로드 (최근 N턴)
 *   ↓ (2) 사용자 메시지 DB 저장
 *   ↓ (3) LawApiService로 관련 법령 검색 (Top 5)
 *   ↓ (4) OpenAiService.chatWithLaws() — 인용 강제 시스템 프롬프트
 *   ↓ (5) AI 답변 + 인용 정보 파싱
 *   ↓ (6) AI 메시지 DB 저장 (citationsJson 포함)
 * AI 답변 반환
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final LawApiService lawApiService;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    /** 프롬프트에 주입할 법령 최대 개수. 비용·컨텍스트 길이 균형. */
    private static final int MAX_LAWS = 5;

    /** 프롬프트에 주입할 이전 메시지 최대 개수 (현재 질문 제외). */
    private static final int MAX_HISTORY = 10;

    @Transactional
    public MessageResponse sendMessage(Long conversationId, Long userId, SendMessageRequest request) {
        Conversation conversation = conversationService.findOwned(conversationId, userId);
        String question = request.getContent();

        // 1) 이전 대화 이력 로드 — 현재 메시지 저장 전에 가져온다
        List<OpenAiService.ChatTurn> history = loadHistory(conversationId);

        // 2) 사용자 메시지 저장
        Message userMessage = Message.builder()
                .conversationId(conversation.getId())
                .role(MessageRole.USER)
                .content(question)
                .citationsJson(null)
                .build();
        messageRepository.save(userMessage);

        // 3) 관련 법령 검색 (도메인 기반 키워드 + 사용자 질문 결합, 실패해도 답변은 진행)
        List<OpenAiService.RetrievedLaw> laws = searchRelevantLaws(conversation.getDomain(), question);
        if (laws.isEmpty()) {
            laws = getStaticFallbackLaws(conversation.getDomain());
            log.info("RAG 검색 결과 없음 — 정적 fallback 법령 사용 (domain={})", conversation.getDomain());
        }
        log.info("RAG 검색 — conversationId={}, domain={}, lawsFound={}",
                conversationId, conversation.getDomain(), laws.size());

        // 4) OpenAI 호출
        OpenAiService.ChatResult result = openAiService.chatWithLaws(question, laws, history);

        // 5) 인용 정보 JSON 직렬화 후 AI 메시지 저장
        String citationsJson = serializeCitations(result.citations());
        Message aiMessage = Message.builder()
                .conversationId(conversation.getId())
                .role(MessageRole.ASSISTANT)
                .content(result.answer())
                .citationsJson(citationsJson)
                .build();
        messageRepository.save(aiMessage);

        // 6) 대화방 updatedAt 갱신 — 목록 정렬이 "최근 대화한 순"이 되도록
        conversation.touch();

        return MessageResponse.from(aiMessage, objectMapper);
    }

    // ============================================================
    // 내부 헬퍼
    // ============================================================

    private List<OpenAiService.ChatTurn> loadHistory(Long conversationId) {
        List<Message> recent = messageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversationId);
        // DB에서 desc로 가져왔으니 asc로 뒤집기
        Collections.reverse(recent);

        // 최대 MAX_HISTORY개만
        int from = Math.max(0, recent.size() - MAX_HISTORY);
        List<Message> sliced = recent.subList(from, recent.size());

        List<OpenAiService.ChatTurn> turns = new ArrayList<>();
        for (Message m : sliced) {
            String role = m.getRole() == MessageRole.USER ? "user" : "assistant";
            turns.add(new OpenAiService.ChatTurn(role, m.getContent()));
        }
        return turns;
    }

    /**
     * 관련 법령 검색.
     * <ol>
     *   <li>사용자 질문에서 핵심 키워드 추출해 본문 검색</li>
     *   <li>결과가 부족하면 도메인 기반 키워드로 보강 (예: LEASE → "임대차")</li>
     * </ol>
     * 이렇게 하면 도메인 내 핵심 법령이 항상 RAG 컨텍스트에 들어가서 GPT가 인용할 수 있다.
     */
    private List<OpenAiService.RetrievedLaw> searchRelevantLaws(LegalDomain domain, String userQuery) {
        List<OpenAiService.RetrievedLaw> result = new ArrayList<>();

        // 1) 사용자 질문 짧게 추려서 1차 검색 (구체적 케이스용 — "임금체불", "보증금" 등)
        String shortQuery = extractShortQuery(userQuery);
        if (!shortQuery.isBlank()) {
            result.addAll(doSearch(shortQuery));
        }

        // 2) 도메인 기반 키워드로 보강 — 항상 핵심 법령이 컨텍스트에 들어가도록
        String domainKeyword = domainKeyword(domain);
        if (!domainKeyword.isBlank() && result.size() < MAX_LAWS) {
            List<OpenAiService.RetrievedLaw> domainLaws = doSearch(domainKeyword);
            for (OpenAiService.RetrievedLaw law : domainLaws) {
                boolean dup = result.stream().anyMatch(r -> r.lawId().equals(law.lawId()));
                if (!dup) {
                    result.add(law);
                    if (result.size() >= MAX_LAWS) break;
                }
            }
        }

        return result;
    }

    private List<OpenAiService.RetrievedLaw> doSearch(String query) {
        try {
            String json = lawApiService.searchLaw(query);
            JsonNode root = objectMapper.readTree(json);
            JsonNode lawsNode = root.path("LawSearch").path("law");

            List<OpenAiService.RetrievedLaw> result = new ArrayList<>();
            if (!lawsNode.isArray()) return result;

            int count = 0;
            for (JsonNode law : lawsNode) {
                if (count >= MAX_LAWS) break;
                String lawId = law.path("법령ID").asText("");
                String lawName = law.path("법령명한글").asText("");
                String ministry = law.path("소관부처명").asText("");
                if (lawId.isBlank() || lawName.isBlank()) continue;
                result.add(new OpenAiService.RetrievedLaw(lawId, lawName, ministry));
                count++;
            }
            return result;
        } catch (Exception e) {
            log.warn("법령 검색 실패 — query='{}'", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * 사용자 질문에서 검색용 짧은 키워드만 추출.
     * 한국어는 형태소 분석이 없으면 정확한 명사 추출이 어려우니, 휴리스틱으로:
     * - 의문사·조사 제거
     * - 처음 등장하는 2~3음절 명사군을 우선 사용
     * - 정 안 되면 처음 15자
     */
    private String extractShortQuery(String userQuery) {
        if (userQuery == null) return "";
        // 너무 짧은 질문이면 그대로
        if (userQuery.length() <= 15) return userQuery.trim();

        // 핵심 명사 우선 키워드 사전 — 자주 등장하는 법률 용어
        String[] keywords = {
                "보증금", "임금체불", "부당해고", "원상복구", "계약해지", "갱신요구",
                "수리비", "도배", "전세", "월세", "임대차", "근로계약",
                "최저임금", "퇴직금", "산재", "환불", "사기", "과실"
        };
        for (String kw : keywords) {
            if (userQuery.contains(kw)) return kw;
        }

        // 키워드 사전 미스 → 처음 15자
        return userQuery.substring(0, 15);
    }

    /**
     * 도메인 → 항상 잡혀야 할 핵심 키워드.
     * 본문 검색에서 이 키워드로는 거의 항상 5건 이상 결과가 나옴.
     */
    private String domainKeyword(LegalDomain domain) {
        return switch (domain) {
            case LEASE -> "임대차";
            case LABOR -> "근로기준법";
            case CONSUMER -> "소비자기본법";
            case TRAFFIC -> "도로교통법";
            case OTHER -> "";
        };
    }

    /** 법령 API 호출이 모두 실패했을 때 사용할 도메인별 정적 fallback 법령 목록. */
    private List<OpenAiService.RetrievedLaw> getStaticFallbackLaws(LegalDomain domain) {
        return switch (domain) {
            case LEASE -> List.of(
                    new OpenAiService.RetrievedLaw("011073", "주택임대차보호법", "법무부"),
                    new OpenAiService.RetrievedLaw("017008", "상가건물 임대차보호법", "법무부"),
                    new OpenAiService.RetrievedLaw("001149", "민법", "법무부")
            );
            case LABOR -> List.of(
                    new OpenAiService.RetrievedLaw("001602", "근로기준법", "고용노동부"),
                    new OpenAiService.RetrievedLaw("001634", "최저임금법", "고용노동부"),
                    new OpenAiService.RetrievedLaw("001835", "고용보험법", "고용노동부")
            );
            case CONSUMER -> List.of(
                    new OpenAiService.RetrievedLaw("001397", "소비자기본법", "공정거래위원회"),
                    new OpenAiService.RetrievedLaw("001351", "전자상거래 등에서의 소비자보호에 관한 법률", "공정거래위원회"),
                    new OpenAiService.RetrievedLaw("001363", "약관의 규제에 관한 법률", "공정거래위원회")
            );
            case TRAFFIC -> List.of(
                    new OpenAiService.RetrievedLaw("001362", "도로교통법", "경찰청"),
                    new OpenAiService.RetrievedLaw("001246", "교통사고처리 특례법", "법무부"),
                    new OpenAiService.RetrievedLaw("001349", "자동차손해배상 보장법", "국토교통부")
            );
            case OTHER -> List.of(
                    new OpenAiService.RetrievedLaw("001149", "민법", "법무부"),
                    new OpenAiService.RetrievedLaw("001276", "형법", "법무부")
            );
        };
    }

    private String serializeCitations(List<OpenAiService.LawCitation> citations) {
        if (citations == null || citations.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (Exception e) {
            log.warn("인용 정보 직렬화 실패", e);
            return "[]";
        }
    }
}
