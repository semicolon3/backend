package com.legalai.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateConversationRequest {

    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;

    private Boolean archived;
}
