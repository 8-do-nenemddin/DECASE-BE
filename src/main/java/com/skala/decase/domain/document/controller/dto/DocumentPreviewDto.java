package com.skala.decase.domain.document.controller.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentPreviewDto {
    private String fileType;
    private String fileName;
    private String docDescription;
    private Long fileSize;
    private String previewUrl;
    private String htmlContent;
    private List<SheetData> sheets; // Excel용

    @Data
    @Builder
    public static class SheetData {
        private String sheetName;
        private List<List<String>> data;
        private int totalRows;
        private int totalCols;
    }
}
