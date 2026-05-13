package com.legalai.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.domain.Message;
import com.legalai.domain.MessageRole;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class MessageResponse {

    private Long id;
    private MessageRole role;
    private String content;
    private List<Citation> citations;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    @Jacksonized
    public static class Citation {
        private String lawId;       // 법령 ID (상세조회용)
        private String lawName;     // 법령명 한글
        private String article;     // 조문 (예: "제7조의2")
    }

    public static MessageResponse from(Message m, ObjectMapper mapper) {
        List<Citation> citations = Collections.emptyList();
        if (m.getCitationsJson() != null && !m.getCitationsJson().isBlank()) {
            try {
                citations = mapper.readValue(m.getCitationsJson(), new TypeReference<>() {});
            } catch (Exception ignored) {
                // 파싱 실패 시 빈 배열로 폴백
            }
        }
        return MessageResponse.builder()
                .id(m.getId())
                .role(m.getRole())
                .content(m.getContent())
                .citations(citations)
                .createdAt(m.getCreatedAt())
                .build();
    }
}
