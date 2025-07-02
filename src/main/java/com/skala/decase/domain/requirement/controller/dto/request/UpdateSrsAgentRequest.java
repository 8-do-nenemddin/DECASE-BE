package com.skala.decase.domain.requirement.controller.dto.request;

/**
 * 요구사항 업데이트 fast api 요청을 위한 request
 */
public record UpdateSrsAgentRequest(
        String reqIdCode,
        String type,
        String level1,
        String level2,
        String level3,
        String priority,
        String difficulty,
        String name,
        String description
) {
}