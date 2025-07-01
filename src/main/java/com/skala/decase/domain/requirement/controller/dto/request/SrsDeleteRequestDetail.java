package com.skala.decase.domain.requirement.controller.dto.request;


public record SrsDeleteRequestDetail(
        String requirement_id,
        String modified_reason
) {
}