package com.legalai.dto.response;

import com.legalai.domain.DocumentTemplate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentTemplateResponse {

    private Long id;
    private String name;
    private String description;
    private String category;
    private String templateContent;
    private String fieldsJson;

    public static DocumentTemplateResponse from(DocumentTemplate template) {
        return DocumentTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory())
                .templateContent(template.getTemplateContent())
                .fieldsJson(template.getFieldsJson())
                .build();
    }
}
