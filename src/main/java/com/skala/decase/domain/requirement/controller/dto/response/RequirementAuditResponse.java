package com.skala.decase.domain.requirement.controller.dto.response;

import java.time.LocalDateTime;
import java.util.Date;

public record RequirementAuditResponse(
        long revisionNumber,
        String reqIdCode,
        String name,
        String description,
        String modReason,
        LocalDateTime revisionDate,
        String modifiedById,
        String modifiedByName,
        String changeType
) {
}
