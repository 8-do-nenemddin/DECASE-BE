package com.skala.decase.domain.project.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public record EditProjectResponseDto (
		Long projectId,
		String name,
		Long scale,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
		Date startDate,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
		Date endDate,
		String description
) {
}