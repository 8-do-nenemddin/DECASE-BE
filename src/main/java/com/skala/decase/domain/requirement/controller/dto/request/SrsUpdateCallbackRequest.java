package com.skala.decase.domain.requirement.controller.dto.request;


public record SrsUpdateCallbackRequest(
        long jobId,
        long memberId,
        String documentId,
        String status,
        SrsUpdateRequest changes
) {
}