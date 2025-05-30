package com.skala.decase.domain.member.mapper;

import com.skala.decase.domain.member.controller.dto.response.DuplicationCheckResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CheckDuplicationMapper {

    public DuplicationCheckResponse success() {
        return new DuplicationCheckResponse(true);
    }

    public DuplicationCheckResponse failure() {
        return new DuplicationCheckResponse(false);
    }
}
