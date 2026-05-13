package com.legalai.dto.response;

import com.legalai.domain.GeneratedDocument;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GeneratedDocumentResponse {

    private Long id;
    private Long templateId;
    private String title;
    private String content;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GeneratedDocumentResponse from(GeneratedDocument doc) {
        return GeneratedDocumentResponse.builder()
                .id(doc.getId())
                .templateId(doc.getTemplateId())
                .title(doc.getTitle())
                .content(doc.getContent())
                .version(doc.getVersion())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
