package com.skala.decase.domain.mockup.controller.dto.request;

/**
 * 목업 생성을 위한 fast api request
 */
public record CreateMockUpSourceRequest(

        int source_page,  //2
        String original_text
) {
}