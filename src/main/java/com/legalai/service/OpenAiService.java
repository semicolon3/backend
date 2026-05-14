package com.legalai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.domain.DocumentType;
import com.legalai.exception.CustomException;
import com.legalai.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions API 클라이언트.
 * <p>
 * - {@link #analyzeDocument} : 업로드된 문서 분석 (요약/엔티티/위험조항 추출)
 * - {@link #chatWithLaws} : RAG 기반 법률 챗봇 (검색된 법령 + 대화이력 + 질문 → 인용 포함 답변)
 * <p>
 * 두 메서드 모두 시스템 프롬프트에 변호사법 가드레일이 박혀 있어,
 * 응답이 항상 일반 법률 정보 수준에 머무르고 개별 사건 자문으로 넘어가지 않도록 설계되어 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    // ============================================================
    // 1. 문서 분석 (DocumentAnalysisExecutor에서 호출)
    // ============================================================

    public String analyzeDocument(String documentText, DocumentType documentType) {
        String systemPrompt = buildAnalysisSystemPrompt();
        String userPrompt = buildAnalysisUserPrompt(documentText, documentType);

        return callChat(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ), ErrorCode.AI_ANALYSIS_FAILED);
    }

    private String buildAnalysisSystemPrompt() {
        return """
                당신은 한국 법률 문서 분석 어시스턴트입니다.
                사용자가 업로드한 법률 문서를 분석하여 '일반적인' 법률 정보를 제공합니다.

                반드시 준수해야 할 원칙:
                1. 당신은 변호사가 아닙니다. 법률 정보 제공만 합니다.
                2. 개별 사건의 결론(승소율·특정 금액 청구 가능성 등)을 단정하지 마세요.
                   "일반적으로", "일반론적으로", "참고로" 같은 어법을 사용합니다.
                3. 관련 법령 조문을 가능한 한 인용하세요 (예: 주택임대차보호법 제7조의2, 근로기준법 제43조).
                4. 응답은 반드시 아래 JSON 형식으로만 반환하세요. 다른 텍스트나 마크다운 금지.

                응답 JSON 형식:
                {
                  "summary": "문서 핵심을 3~4문장으로 요약. 마지막 문장에 '본 정보는 법률 자문이 아니며, 정확한 판단은 변호사 상담을 권장합니다.' 면책 문구를 포함하세요.",
                  "entities": {
                    "당사자": ["문서에 등장하는 사람/조직 이름"],
                    "날짜": ["계약일·시행일 등 주요 일자"],
                    "금액": ["보증금·임차료·청구금액 등 주요 금액"]
                  },
                  "riskClauses": [
                    {
                      "clauseTitle": "주의가 필요한 조항/내용 제목 (예: 제5조 보증금 반환)",
                      "originalText": "문서에서 해당 조항의 실제 원문 텍스트를 그대로 복사하세요. 없으면 빈 문자열.",
                      "description": "왜 주의해야 하는지 일반론적 설명 + 관련 법령 인용",
                      "severity": "LOW | MEDIUM | HIGH"
                    }
                  ]
                }

                추출할 정보가 없으면 배열은 [], 객체는 {}로 비워두세요.
                severity는 반드시 LOW, MEDIUM, HIGH 중 하나여야 합니다.
                originalText는 문서 원문에서 해당 조항 텍스트를 그대로 가져오세요 (요약 금지).
                """;
    }

    private String buildAnalysisUserPrompt(String documentText, DocumentType documentType) {
        String typeLabel = switch (documentType) {
            case CONTRACT -> "계약서";
            case KAKAO_CHAT -> "카카오톡 대화 캡쳐";
            case RECEIPT -> "영수증/이체내역";
            case OTHER -> "기타 문서";
        };
        return """
                문서 유형: %s

                문서 내용:
                %s
                """.formatted(typeLabel, documentText);
    }

    // ============================================================
    // 2. RAG 챗봇 (RagChatService에서 호출)
    // ============================================================

    /**
     * RAG 기반 챗봇 답변 생성.
     *
     * @param question 사용자의 현재 질문
     * @param laws     법령 검색기에서 가져온 관련 법령들 (Top N, 보통 3~5개)
     * @param history  이전 대화 이력 (시간 순서, 최근 10턴 정도)
     * @return 답변 텍스트 + 인용 법령 리스트
     */
    public ChatResult chatWithLaws(String question, List<RetrievedLaw> laws, List<ChatTurn> history) {
        log.info(">>> [v2] chatWithLaws 진입 — laws={}, history={}",
                laws == null ? 0 : laws.size(),
                history == null ? 0 : history.size());

        String systemPrompt = buildChatSystemPrompt(laws);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatTurn turn : history) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }
        messages.add(Map.of("role", "user", "content", question));

        String responseJson = callChat(messages, ErrorCode.AI_CHAT_FAILED);
        log.info(">>> [v2] GPT 응답 원본: {}", responseJson);

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String answer = root.path("answer").asText("");

            List<LawCitation> citations = new ArrayList<>();
            JsonNode citationsNode = root.path("citations");
            if (citationsNode.isArray()) {
                for (JsonNode c : citationsNode) {
                    String lawId = c.path("lawId").asText("");
                    String lawName = c.path("lawName").asText("");
                    if (lawId.isBlank() && lawName.isBlank()) continue;
                    citations.add(new LawCitation(
                            lawId,
                            lawName,
                            c.path("article").asText("")
                    ));
                }
            }

            // 후처리 안전망: citations가 비었으면 자동 보강
            // (1) retrieve된 법령 중 answer 본문에 언급된 법령 추출
            // (2) 그래도 비면 첫 번째 검색 결과를 best-effort로 추가
            // (3) laws도 없으면 answer에서 알려진 법령명 패턴 매칭
            if (citations.isEmpty()) {
                if (laws != null && !laws.isEmpty()) {
                    for (RetrievedLaw law : laws) {
                        if (law.lawName() != null && !law.lawName().isBlank()
                                && answer.contains(law.lawName())) {
                            citations.add(new LawCitation(law.lawId(), law.lawName(), ""));
                            if (citations.size() >= 3) break;
                        }
                    }
                    if (citations.isEmpty()) {
                        RetrievedLaw first = laws.get(0);
                        citations.add(new LawCitation(first.lawId(), first.lawName(), ""));
                        log.info("citations 자동 보강 (best-effort) — lawId={}", first.lawId());
                    } else {
                        log.info("citations 자동 보강 (본문 언급 추출) — {}건", citations.size());
                    }
                } else {
                    // laws도 없는 경우: answer 본문에서 알려진 법령명 패턴 매칭
                    String[][] knownLaws = {
                            {"011073", "주택임대차보호법"}, {"017008", "상가건물 임대차보호법"},
                            {"001602", "근로기준법"}, {"001634", "최저임금법"},
                            {"001397", "소비자기본법"}, {"001362", "도로교통법"},
                            {"001149", "민법"}, {"001276", "형법"},
                            {"001246", "교통사고처리 특례법"}, {"001363", "약관의 규제에 관한 법률"}
                    };
                    for (String[] law : knownLaws) {
                        if (answer.contains(law[1])) {
                            citations.add(new LawCitation(law[0], law[1], ""));
                            if (citations.size() >= 3) break;
                        }
                    }
                    if (!citations.isEmpty()) {
                        log.info("citations 자동 보강 (알려진 법령명 패턴) — {}건", citations.size());
                    }
                }
            }

            return new ChatResult(answer, citations);
        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 실패: {}", responseJson, e);
            throw new CustomException(ErrorCode.AI_CHAT_FAILED);
        }
    }

    private String buildChatSystemPrompt(List<RetrievedLaw> laws) {
        StringBuilder lawsText = new StringBuilder();
        if (laws != null && !laws.isEmpty()) {
            for (RetrievedLaw law : laws) {
                lawsText.append("- [").append(law.lawId()).append("] ")
                        .append(law.lawName()).append("\n");
                if (law.summary() != null && !law.summary().isBlank()) {
                    lawsText.append("  요약/소관부처: ").append(law.summary()).append("\n");
                }
            }
        } else {
            lawsText.append("(관련 법령 검색 결과 없음)");
        }

        return """
                당신은 한국 법률 정보 어시스턴트입니다. 사용자의 질문에 대해 '일반적인' 법률 정보를 제공합니다.

                반드시 준수해야 할 원칙:
                1. 당신은 변호사가 아닙니다. 법률 정보 제공만 합니다 (변호사법 §109 준수).
                2. 개별 사건의 결론을 단정하지 마세요. 승소 가능성·구체 금액 청구 가능성을 단언하지 마세요.
                   "일반적으로", "일반론적으로", "참고로", "~할 수 있습니다" 같은 어법을 사용합니다.

                3. **인용 필수** — 답변(answer)과 citations에 반드시 아래 '관련 법령' 목록에서 1개 이상의 법령을 포함해야 합니다.
                   - 사용자 질문과 가장 관련성 높은 법령 1~3개를 골라 citations에 넣으세요.
                   - 가장 관련된 조문 번호도 함께 명시 (모르면 article은 빈 문자열).
                   - 답변 본문(answer) 안에서도 "주택임대차보호법에 따르면…"처럼 자연스럽게 법령명을 언급하세요.
                   - 관련 법령 목록 자체가 "(관련 법령 검색 결과 없음)"인 경우에만 citations를 빈 배열로 두세요.

                4. 답변 마지막 문장에 반드시 다음 면책 문구를 그대로 포함하세요:
                   "본 정보는 법률 자문이 아니며, 정확한 판단은 변호사 상담을 권장합니다."

                5. 응답은 반드시 아래 JSON 형식으로만 반환합니다 (다른 텍스트/마크다운 금지).

                관련 법령 (이 목록에서만 인용 가능):
                %s

                응답 JSON 형식:
                {
                  "answer": "사용자 질문에 대한 답변 (본문에서도 법령명 언급 + 마지막에 면책 문구).",
                  "citations": [
                    {
                      "lawId": "위 관련 법령의 lawId를 그대로 복사 (예: '001132')",
                      "lawName": "법령명 그대로 복사",
                      "article": "인용한 조문 (예: '제7조의2'). 특정 조문을 모르면 빈 문자열"
                    }
                  ]
                }

                ── 예시 (이렇게 답해야 합니다) ──
                관련 법령 목록에 [001132] 주택임대차보호법, [002311] 약관의 규제에 관한 법률 이 있고
                사용자가 "보증금 돌려받을 수 있나요?"라고 물으면:

                {
                  "answer": "일반적으로 주택임대차보호법 제7조의2에 따르면 임차인이 통상의 사용에 따른 마모에 대해서는 비용을 부담할 의무가 없습니다. 약관의 규제에 관한 법률 제6조에서 약관의 부당조항도 무효가 될 수 있습니다. 본 정보는 법률 자문이 아니며, 정확한 판단은 변호사 상담을 권장합니다.",
                  "citations": [
                    {"lawId": "001132", "lawName": "주택임대차보호법", "article": "제7조의2"},
                    {"lawId": "002311", "lawName": "약관의 규제에 관한 법률", "article": "제6조"}
                  ]
                }

                ★ 핵심: answer에서 법령을 언급했다면 반드시 citations에도 같은 법령이 들어가야 합니다. citations를 빈 배열로 두지 마세요.
                """.formatted(lawsText.toString());
    }

    // ============================================================
    // 공통 호출 헬퍼
    // ============================================================

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String callChat(List<Map<String, String>> messages, ErrorCode failureCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.3,
                "response_format", Map.of("type", "json_object"),
                "messages", messages
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            Map response = restTemplate.postForObject(apiUrl, entity, Map.class);

            if (response == null) throw new CustomException(failureCode);

            List<?> choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) throw new CustomException(failureCode);

            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
            return (String) message.get("content");

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI 호출 실패", e);
            throw new CustomException(failureCode);
        }
    }

    // ============================================================
    // 챗봇 관련 DTO (RagChatService에서도 사용)
    // ============================================================

    /** 이전 대화 한 턴 (role은 "user" 또는 "assistant"). */
    public record ChatTurn(String role, String content) {
        public static ChatTurn user(String content) { return new ChatTurn("user", content); }
        public static ChatTurn assistant(String content) { return new ChatTurn("assistant", content); }
    }

    /** 검색기에서 추출한 법령 요약 (LLM 프롬프트에 들어감). */
    public record RetrievedLaw(String lawId, String lawName, String summary) {}

    /** AI 응답에 포함된 인용 법령 정보. */
    public record LawCitation(String lawId, String lawName, String article) {}

    /** chatWithLaws의 최종 반환값. */
    public record ChatResult(String answer, List<LawCitation> citations) {
        public List<LawCitation> citations() {
            return citations == null ? Collections.emptyList() : citations;
        }
    }
}
