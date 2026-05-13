package com.legalai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class SendMessageRequest {

    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 4000, message = "메시지는 4000자 이하여야 합니다.")
    private String content;
}
