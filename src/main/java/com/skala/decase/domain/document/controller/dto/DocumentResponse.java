package com.skala.decase.domain.document.controller.dto;

public record DocumentResponse(
        String docId,
        String fileName,
        String docDescription
) {
}