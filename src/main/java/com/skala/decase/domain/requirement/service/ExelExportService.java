package com.skala.decase.domain.requirement.service;

import com.skala.decase.domain.project.controller.dto.response.DocumentResponse;
import com.skala.decase.domain.project.controller.dto.response.MappingTableResponseDto;
import com.skala.decase.domain.requirement.controller.dto.response.MatrixResponse;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementResponse;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementWithSourceResponse;
import com.skala.decase.domain.requirement.controller.dto.response.SourceResponse;
import com.skala.decase.domain.requirement.domain.Reception;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
public class ExelExportService {
    /**
     * 요구사항 데이터를 CSV 형식으로 변환
     */
    public byte[] generateExcelFile(List<RequirementResponse> responses) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("요구사항 정의서");

        // 헤더 스타일 생성
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // 데이터 스타일 생성
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setVerticalAlignment(VerticalAlignment.TOP);
        dataStyle.setWrapText(true); // 텍스트 줄바꿈
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // 헤더 행 생성
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "요구사항 ID", "요구사항 유형", "대분류", "중분류", "소분류",
                "요구사항 명", "요구사항 설명", "중요도", "난이도", "출처",
                "관리 구분", "수용 여부", "변경 이력", "최종 변경 일자"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 데이터 행 생성
        int rowNum = 1;
        for (RequirementResponse response : responses) {
            Row row = sheet.createRow(rowNum++);

            // 각 셀에 데이터 입력
            createCell(row, 0, response.getReqIdCode(), dataStyle);
            createCell(row, 1, convertTypeToKorean(response.getType()), dataStyle);
            createCell(row, 2, response.getLevel1(), dataStyle);
            createCell(row, 3, response.getLevel2(), dataStyle);
            createCell(row, 4, response.getLevel3(), dataStyle);
            createCell(row, 5, response.getName(), dataStyle);
            createCell(row, 6, response.getDescription(), dataStyle);
            createCell(row, 7, convertPriorityToKorean(response.getPriority()), dataStyle);
            createCell(row, 8, convertDifficultyToKorean(response.getDifficulty()), dataStyle);
            createCell(row, 9, formatSources(response.getSources()), dataStyle);
            createCell(row, 10, response.getRevType(), dataStyle); //관리 구분
            createCell(row, 11, response.getReception(), dataStyle); //수용 여부
            createCell(row, 12, formatModificationHistory(response.getModReason()), dataStyle);
            createCell(row, 13, formatDate(response.getModifiedDate()), dataStyle);
        }

        // 컬럼 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // 최대 너비 제한 (너무 넓어지는 것 방지)
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth > 15000) { // 약 100글자 정도
                sheet.setColumnWidth(i, 15000);
            }
        }

        // 행 높이 설정 (내용이 많은 경우를 위해)
        sheet.setDefaultRowHeight((short) 600); // 기본 행 높이 설정

        // Excel 파일을 바이트 배열로 변환
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    // 셀 생성 헬퍼 메서드
    private void createCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    /**
     * 요구사항 유형을 한글로 변환
     */
    private String convertTypeToKorean(String type) {
        if ("FR".equals(type)) {
            return "기능";
        } else if ("NFR".equals(type)) {
            return "비기능";
        }
        return type;
    }

    private String convertAcceptedTypeToKorean(Reception reception) {
        if (reception == Reception.ACCEPTED) {
            return "수용";
        }
        return "미수용";
    }

    /**
     * 우선순위를 한글로 변환
     */
    private String convertPriorityToKorean(String priority) {
        switch (priority) {
            case "HIGH":
                return "상";
            case "MIDDLE":
                return "중";
            case "LOW":
                return "하";
            default:
                return priority;
        }
    }

    /**
     * 난이도를 한글로 변환
     */
    private String convertDifficultyToKorean(String difficulty) {
        switch (difficulty) {
            case "HIGH":
                return "상";
            case "MIDDLE":
                return "중";
            case "LOW":
                return "하";
            default:
                return difficulty;
        }
    }

    /**
     * 출처 정보를 포맷팅
     */
    private String formatSources(List<SourceResponse> sources) {
        if (sources == null || sources.isEmpty()) {
            return "";
        }

        return sources.stream()
                .map(source -> String.format("%s (%d페이지)\n%s",
                        source.docId(),
                        source.pageNum(),
                        source.relSentence()))
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 출처 ID를 포맷팅
     */
    private String formatSourceIds(List<SourceResponse> sources) {
        if (sources == null || sources.isEmpty()) {
            return "";
        }

        return sources.stream()
                .map(source -> String.valueOf(source.sourceId()))
                .collect(Collectors.joining(", "));
    }

    /**
     * 변경이력을 포맷팅
     */
    private String formatModificationHistory(List<String> modReasons) {
        if (modReasons == null || modReasons.isEmpty()) {
            return "";
        }

        return modReasons.stream()
                .filter(reason -> reason != null && !reason.trim().isEmpty())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 날짜를 포맷팅
     */
    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        return dateTime.format(formatter);
    }

    public byte[] generateMatrixExcelFile(List<MatrixResponse> responses) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("요구사항 추적 매트릭스");

        // 헤더 스타일 생성
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // 데이터 스타일 생성
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setVerticalAlignment(VerticalAlignment.TOP);
        dataStyle.setWrapText(true); // 텍스트 줄바꿈
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // 헤더 행 생성
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "요구 사항ID", "level1", "level2", "level3", "요구 사항명", "요구 사항 설명", "수용 여부",
                "테이블 ID", "화면 ID", "프로그램 ID", "인터 페이스 ID", "배치 ID",
                "단위 테스트 ID", "통합 테스트 ID", "인수 테스트 ID"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (MatrixResponse item : responses) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getReqIdCode());
            row.createCell(1).setCellValue(item.getLevel1());
            row.createCell(2).setCellValue(item.getLevel2());
            row.createCell(3).setCellValue(item.getLevel3());
            row.createCell(4).setCellValue(item.getName());
            row.createCell(5).setCellValue(item.getDescription());
            row.createCell(6).setCellValue(convertAcceptedTypeToKorean(item.getReception()));
            row.createCell(7).setCellValue(item.getTableId());
            row.createCell(8).setCellValue(item.getUiId());
            row.createCell(9).setCellValue(item.getProgramId());
            row.createCell(10).setCellValue(item.getBatchId());
            row.createCell(11).setCellValue(item.getUnitTestId());
            row.createCell(12).setCellValue(item.getIntegrationTest());
            row.createCell(13).setCellValue(item.getAcceptanceTest());
        }

        // 컬럼 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // 최대 너비 제한 (너무 넓어지는 것 방지)
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth > 15000) { // 약 100글자 정도
                sheet.setColumnWidth(i, 15000);
            }
        }

        // 행 높이 설정 (내용이 많은 경우를 위해)
        sheet.setDefaultRowHeight((short) 600); // 기본 행 높이 설정

        // Excel 파일을 바이트 배열로 변환
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    // 조견표 출력
    public byte[] createMappingTableToExcel(List<MappingTableResponseDto> responses) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("조견표");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // 데이터 스타일 생성
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setVerticalAlignment(VerticalAlignment.TOP);
        dataStyle.setWrapText(true); // 텍스트 줄바꿈
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // 1. 헤더 작성
        Row headerRow = sheet.createRow(0);
        String[] headers = {"요구사항 ID", "요구사항명", "설명", "출처 문서명", "페이지 번호", "관련 문장"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 2. 내용 채우기
        int rowIdx = 1;
        for (MappingTableResponseDto dto : responses) {
            List<DocumentResponse> docs = dto.document();
            if (docs == null || docs.isEmpty()) {
                // 문서가 없을 때 한 줄만 작성
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.req_code());
                row.createCell(1).setCellValue(dto.name());
                row.createCell(2).setCellValue(dto.description());
                row.createCell(3).setCellValue("");
                row.createCell(4).setCellValue("");
                row.createCell(5).setCellValue("");
            } else {
                int startRow = rowIdx; // 병합 시작 행

                for (DocumentResponse doc : docs) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(3).setCellValue(doc.docName());
                    row.createCell(4).setCellValue(doc.pageNum());
                    row.createCell(5).setCellValue(doc.relSentence());
                }

                // 병합할 행이 여러 줄일 때만 병합 처리
                if (docs.size() > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(startRow, rowIdx - 1, 0, 0)); // req_code
                    sheet.addMergedRegion(new CellRangeAddress(startRow, rowIdx - 1, 1, 1)); // name
                    sheet.addMergedRegion(new CellRangeAddress(startRow, rowIdx - 1, 2, 2)); // description
                }

                // 병합 셀은 첫 줄에만 값 넣기
                Row firstRow = sheet.getRow(startRow);
                firstRow.createCell(0).setCellValue(dto.req_code());
                firstRow.createCell(1).setCellValue(dto.name());
                firstRow.createCell(2).setCellValue(dto.description());
            }
        }

        // 컬럼 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // 최대 너비 제한 (너무 넓어지는 것 방지)
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth > 15000) { // 약 100글자 정도
                sheet.setColumnWidth(i, 15000);
            }
        }

        // 행 높이 설정 (내용이 많은 경우를 위해)
        sheet.setDefaultRowHeight((short) 600); // 기본 행 높이 설정

        // 3. 엑셀 파일을 byte[]로 변환 (서버에서 다운로드 응답용)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return out.toByteArray();
    }
}
