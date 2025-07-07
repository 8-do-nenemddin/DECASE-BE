package com.skala.decase.domain.project.controller.dto.response;

public record PermissionResponse(
		String permission,
		Boolean isAdmin
) {
}
