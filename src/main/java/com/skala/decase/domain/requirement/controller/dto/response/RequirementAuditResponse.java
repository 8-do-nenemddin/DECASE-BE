package com.skala.decase.domain.requirement.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Date;

public record RequirementAuditResponse(
        long revisionNumber,
        String reqIdCode,
        String name,
        String description,
        String modReason,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime revisionDate,
        String modifiedById,
        String modifiedByName,
        String changeType
) {
}
