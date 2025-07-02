package com.skala.decase.domain.project.controller.dto.request;

import java.util.List;

public record RequirementDescriptionRequest(
		List<String> requirementIds
) {}
