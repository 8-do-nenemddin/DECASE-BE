package com.skala.decase.domain.member.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeleteRequest(
        @NotBlank(message = "본인 확인을 위한 비밀번호를 입력해주세요.")
        String password
) {
}
