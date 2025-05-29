package com.skala.decase.domain.document.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentResponse {
    private String docId;
    private String fileName;
}