package com.legalai.dto.response;

import com.legalai.domain.Conversation;
import com.legalai.domain.LegalDomain;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대화 상세 조회 응답 — 대화 메타데이터 + 메시지 전체 포함.
 */
@Getter
@Builder
public class ConversationDetailResponse {

    private Long id;
    private String title;
    private LegalDomain domain;
    private boolean archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MessageResponse> messages;

    public static ConversationDetailResponse of(Conversation c, List<MessageResponse> messages) {
        return ConversationDetailResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .domain(c.getDomain())
                .archived(c.isArchived())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .messages(messages)
                .build();
    }
}
