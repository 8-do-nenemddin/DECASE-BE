package com.skala.decase.domain.requirement.controller.dto.request;

import java.util.List;

public record SrsUpdateRequest(
        List<SrsUpdateRequestDetail> to_add,
        List<SrsUpdateRequestDetail> to_update,
        List<SrsDeleteRequestDetail> to_delete
) {
}