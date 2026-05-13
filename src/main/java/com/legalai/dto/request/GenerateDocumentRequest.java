package com.legalai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.Map;

@Getter
public class GenerateDocumentRequest {

    @NotNull(message = "템플릿 ID는 필수입니다.")
    private Long templateId;

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    // 템플릿 플레이스홀더를 채울 값 (예: {"발신인_이름": "홍길동", "수신인_이름": "이영희"})
    private Map<String, String> fields;
}
