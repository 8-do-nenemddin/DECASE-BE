package com.skala.decase.domain.document.service;

import com.skala.decase.domain.document.controller.dto.DocumentPreviewDto;
import com.skala.decase.domain.document.domain.Document;
import com.skala.decase.domain.document.exception.DocumentException;
import com.skala.decase.domain.document.repository.DocumentRepository;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentPreviewService {

    private final DocumentRepository documentRepository;

    /**
     * 파일 프리뷰 전송
     */
    public Resource previewDocument(String docId) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new DocumentException("문서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        Path filePath = Paths.get(doc.getPath());
        if (!Files.exists(filePath)) {
            throw new DocumentException("파일이 존재하지 않습니다.", HttpStatus.NOT_FOUND);
        }

        try {
            return new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new DocumentException("파일 경로가 잘못되었습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public DocumentPreviewDto getDocumentPreview(String docId) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new DocumentException("문서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        Path filePath = Paths.get(doc.getPath());
        if (!Files.exists(filePath)) {
            throw new DocumentException("파일이 존재하지 않습니다.", HttpStatus.NOT_FOUND);
        }

        String fileName = doc.getName();
        String fileExtension = getFileExtension(fileName).toLowerCase();

        try {
            long fileSize = Files.size(filePath);

            DocumentPreviewDto.DocumentPreviewDtoBuilder builder = DocumentPreviewDto.builder()
                    .fileType(fileExtension)
                    .docDescription(doc.getDocDescription())
                    .fileName(fileName)
                    .fileSize(fileSize);

            switch (fileExtension) {
                case "pdf":
                    return builder
                            .previewUrl("/api/v1/documents/" + docId + "/preview")
                            .build();

                case "docx":
                    String htmlContent = convertDocxToHtml(filePath);
                    return builder
                            .htmlContent(htmlContent)
                            .build();
                case "csv":
                case "xlsx":
                case "xls":
                    List<DocumentPreviewDto.SheetData> sheets = convertExcelToData(filePath, fileExtension);
                    return builder
                            .sheets(sheets)
                            .build();

                default:
                    return builder.build();
            }

        } catch (IOException e) {
            throw new DocumentException("파일을 읽는 중 오류가 발생했습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String convertDocxToHtml(Path filePath) throws IOException {
        StringBuilder htmlBuilder = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XWPFDocument document = new XWPFDocument(fis)) {

            htmlBuilder.append("<div class='docx-content'>");

            // 문단 처리
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    // 간단한 스타일 적용
                    if (paragraph.getStyle() != null && paragraph.getStyle().contains("Heading")) {
                        htmlBuilder.append("<h3>").append(escapeHtml(text)).append("</h3>");
                    } else {
                        htmlBuilder.append("<p>").append(escapeHtml(text)).append("</p>");
                    }
                }
            }

            // 표 처리
            List<XWPFTable> tables = document.getTables();
            for (XWPFTable table : tables) {
                htmlBuilder.append("<table class='docx-table'>");

                List<XWPFTableRow> rows = table.getRows();
                boolean isFirstRow = true;

                for (XWPFTableRow row : rows) {
                    htmlBuilder.append("<tr>");
                    List<XWPFTableCell> cells = row.getTableCells();

                    for (XWPFTableCell cell : cells) {
                        String cellText = cell.getText();
                        if (isFirstRow) {
                            htmlBuilder.append("<th>").append(escapeHtml(cellText)).append("</th>");
                        } else {
                            htmlBuilder.append("<td>").append(escapeHtml(cellText)).append("</td>");
                        }
                    }
                    htmlBuilder.append("</tr>");
                    isFirstRow = false;
                }

                htmlBuilder.append("</table>");
            }

            htmlBuilder.append("</div>");
        }

        return htmlBuilder.toString();
    }

    private List<DocumentPreviewDto.SheetData> convertExcelToData(Path filePath, String extension) throws IOException {
        List<DocumentPreviewDto.SheetData> sheetDataList = new ArrayList<>();

        if ("csv".equals(extension)) {
            List<String[]> rows = Files.readAllLines(filePath).stream()
                    .map(line -> line.split(",", -1))  // 빈 값도 포함
                    .collect(Collectors.toList());

            List<List<String>> rowData = new ArrayList<>();
            for (String[] row : rows) {
                rowData.add(List.of(row));
            }

            sheetDataList.add(
                    DocumentPreviewDto.SheetData.builder()
                            .sheetName("CSV Preview")
                            .data(rowData)
                            .build()
            );

            return sheetDataList;
        }

        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            Workbook workbook = extension.equals("xlsx") ?
                    new XSSFWorkbook(fis) :
                    new HSSFWorkbook(fis);

            for (Sheet sheet : workbook) {
                List<List<String>> data = new ArrayList<>();
                for (Row row : sheet) {
                    List<String> rowData = new ArrayList<>();
                    for (Cell cell : row) {
                        switch (cell.getCellType()) {
                            case STRING:
                                rowData.add(cell.getStringCellValue());
                                break;
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    rowData.add(cell.getDateCellValue().toString());
                                } else {
                                    rowData.add(String.valueOf(cell.getNumericCellValue()));
                                }
                                break;
                            case BOOLEAN:
                                rowData.add(String.valueOf(cell.getBooleanCellValue()));
                                break;
                            case FORMULA:
                                rowData.add(cell.getCellFormula());
                                break;
                            case BLANK:
                                rowData.add("");
                                break;
                            default:
                                rowData.add("");
                        }
                    }
                    data.add(rowData);
                }

                sheetDataList.add(DocumentPreviewDto.SheetData.builder()
                        .sheetName(sheet.getSheetName())
                        .data(data)
                        .build());
            }
        }

        return sheetDataList;
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}