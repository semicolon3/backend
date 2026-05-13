package com.legalai.dto.response;

import com.legalai.domain.Conversation;
import com.legalai.domain.LegalDomain;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConversationResponse {

    private Long id;
    private String title;
    private LegalDomain domain;
    private boolean archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ConversationResponse from(Conversation c) {
        return ConversationResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .domain(c.getDomain())
                .archived(c.isArchived())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
