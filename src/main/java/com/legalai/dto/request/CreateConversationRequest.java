package com.legalai.dto.request;

import com.legalai.domain.LegalDomain;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateConversationRequest {

    @NotNull(message = "법률 도메인은 필수입니다.")
    private LegalDomain domain;

    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;  // null이면 도메인 기반 기본 제목 자동 생성
}
