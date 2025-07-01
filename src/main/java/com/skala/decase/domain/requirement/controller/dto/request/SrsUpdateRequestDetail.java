package com.skala.decase.domain.requirement.controller.dto.request;


public record SrsUpdateRequestDetail(
        String requirement_id,
        String requirement_name,
        String type,
        String description,
        String target_page,
        String level1,
        String level2,
        String level3,
        String importance,
        String difficulty,
        String modified_reason
) {
}