package com.skala.decase.domain.project.controller.dto.response;

import java.util.List;

public record RequirementDescriptionsResponse(
		List<RequirementDescriptionResponse> descriptions
) {}
