package com.legalai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.domain.Conversation;
import com.legalai.domain.LegalDomain;
import com.legalai.domain.Message;
import com.legalai.dto.request.CreateConversationRequest;
import com.legalai.dto.request.UpdateConversationRequest;
import com.legalai.dto.response.ConversationDetailResponse;
import com.legalai.dto.response.ConversationResponse;
import com.legalai.dto.response.MessageResponse;
import com.legalai.exception.CustomException;
import com.legalai.exception.ErrorCode;
import com.legalai.repository.ConversationRepository;
import com.legalai.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 대화방 CRUD. 실제 RAG 답변 생성은 {@link RagChatService} 담당.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ConversationResponse create(CreateConversationRequest request, Long userId) {
        String title = (request.getTitle() == null || request.getTitle().isBlank())
                ? defaultTitle(request.getDomain())
                : request.getTitle();

        Conversation c = Conversation.builder()
                .userId(userId)
                .title(title)
                .domain(request.getDomain())
                .build();
        conversationRepository.save(c);
        return ConversationResponse.from(c);
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponse> list(Long userId, Pageable pageable) {
        return conversationRepository
                .findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId, pageable)
                .map(ConversationResponse::from);
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getDetail(Long conversationId, Long userId) {
        Conversation c = findOwned(conversationId, userId);
        List<MessageResponse> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(m -> MessageResponse.from(m, objectMapper))
                .toList();
        return ConversationDetailResponse.of(c, messages);
    }

    @Transactional
    public ConversationResponse update(Long conversationId, UpdateConversationRequest request, Long userId) {
        Conversation c = findOwned(conversationId, userId);
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            c.rename(request.getTitle());
        }
        if (request.getArchived() != null) {
            c.setArchived(request.getArchived());
        }
        return ConversationResponse.from(c);
    }

    @Transactional
    public void delete(Long conversationId, Long userId) {
        Conversation c = findOwned(conversationId, userId);
        c.softDelete();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long conversationId, Long userId) {
        findOwned(conversationId, userId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(m -> MessageResponse.from(m, objectMapper))
                .toList();
    }

    // ============================================================
    // 내부 헬퍼
    // ============================================================

    Conversation findOwned(Long conversationId, Long userId) {
        Conversation c = conversationRepository.findByIdAndDeletedAtIsNull(conversationId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONVERSATION_NOT_FOUND));
        if (!c.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.CONVERSATION_ACCESS_DENIED);
        }
        return c;
    }

    private String defaultTitle(LegalDomain domain) {
        return switch (domain) {
            case LEASE -> "임대차 상담";
            case LABOR -> "근로 상담";
            case CONSUMER -> "소비자 상담";
            case TRAFFIC -> "교통 상담";
            case OTHER -> "법률 상담";
        };
    }
}
