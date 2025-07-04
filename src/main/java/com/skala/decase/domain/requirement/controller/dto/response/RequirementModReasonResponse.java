package com.skala.decase.domain.requirement.controller.dto.response;

import java.time.LocalDateTime;

public record RequirementModReasonResponse(
        String reqIdCode,
        String ModReason,
        LocalDateTime revisionDate
) {
}
