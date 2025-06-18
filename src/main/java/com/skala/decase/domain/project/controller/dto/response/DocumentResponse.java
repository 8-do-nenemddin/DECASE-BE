package com.skala.decase.domain.project.controller.dto.response;

public record DocumentResponse(
        String docName,
        Integer pageNum,
        String relSentence
) {
}
