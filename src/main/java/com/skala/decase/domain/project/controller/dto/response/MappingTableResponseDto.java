package com.skala.decase.domain.project.controller.dto.response;

import java.util.List;

public record MappingTableResponseDto(
		String req_code,
		String name,
		String description,
		List<DocumentResponse> document
) {}