package com.skala.decase.domain.project.controller.dto.request;

import com.skala.decase.domain.project.domain.Permission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMemberProjectRequest(
        @NotNull(message = "Admin의 Id를 입력해주세요.")
        long adminId,

        @NotBlank(message = "초대할 이메일을 입력해주세요.")
        @Size(max = 50, message = "이메일은 50자 이하로 제한합니다.")
        String email,
        Permission permission // enum과 동일한 형태로 받으면 됩니다.
) {
}
