package com.skala.decase.domain.member.mapper;

import com.skala.decase.domain.member.controller.dto.response.DeleteResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DeleteMapper {

    public DeleteResponse success() {
        return new DeleteResponse(true);
    }
}
