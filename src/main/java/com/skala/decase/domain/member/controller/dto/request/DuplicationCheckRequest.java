package com.skala.decase.domain.member.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DuplicationCheckRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        String id
) {
}
