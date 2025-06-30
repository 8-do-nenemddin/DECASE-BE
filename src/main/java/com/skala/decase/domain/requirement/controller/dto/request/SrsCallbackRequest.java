package com.skala.decase.domain.requirement.controller.dto.request;

import java.util.List;

public record SrsCallbackRequest(
        long member_id,
        String document_id,
        String status,
        List<CreateRfpRequest> srs
) {
}