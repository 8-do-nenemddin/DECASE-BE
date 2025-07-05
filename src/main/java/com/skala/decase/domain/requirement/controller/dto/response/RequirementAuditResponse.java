package com.skala.decase.domain.requirement.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.skala.decase.domain.requirement.domain.Difficulty;
import com.skala.decase.domain.requirement.domain.Priority;
import com.skala.decase.domain.requirement.domain.Reception;
import com.skala.decase.domain.requirement.domain.RequirementType;

import java.time.LocalDateTime;

public record RequirementAuditResponse(
        long revisionNumber,
        long version,
        String reqIdCode,
        RequirementType type,
        String name,
        String description,
        String level1,
        String level2,
        String level3,
        Priority priority,
        Difficulty difficulty,
        Reception reception,
        String modReason,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime revisionDate,
        String modifiedById,
        String modifiedByName,
        String changeType
) {
}
