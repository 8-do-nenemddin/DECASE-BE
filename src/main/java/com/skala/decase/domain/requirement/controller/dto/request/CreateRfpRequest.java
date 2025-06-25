package com.skala.decase.domain.requirement.controller.dto.request;

import java.util.List;

public record CreateRfpRequest(
        String requirement_name,
        String type,
        List<SourceCallbackReq> sources,
        String description,
        String target_page,
        String category_large,
        String category_medium,
        String category_small,
        String importance,
        String difficulty,
        String requirement_id_prefix,
        String requirement_id
) {
}