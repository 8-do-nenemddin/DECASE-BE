package com.skala.decase.domain.mockup.controller.dto.request;


/**
 * 화면 정의서 생성을 위한 fast api request
 */
public record SpecGenerationRequest(
        String mockup_dir,
        String output_dir
) {
}