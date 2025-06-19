package com.skala.decase.domain.requirement.controller;

import com.skala.decase.domain.requirement.controller.dto.response.MatrixResponse;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementAuditResponse;
import com.skala.decase.domain.requirement.service.ExelExportService;
import com.skala.decase.domain.requirement.service.RequirementAuditService;
import com.skala.decase.global.model.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Tag(name = "Matrix API", description = "요구사항 추적 매트릭스 관리를 위한 api 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/matrix/")
public class RequirementAuditController {

    private final ExelExportService exelExportService;
    private final RequirementAuditService requirementAuditService;

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<List<RequirementAuditResponse>>> findAllByProjectId(@PathVariable("projectId") long projectId) {
        List<RequirementAuditResponse> responses = requirementAuditService.findAllByProjectId(projectId);

        return ResponseEntity.ok()
                .body(ApiResponse.success(responses));
    }

    @GetMapping("/projects/{projectId}/srs/{reqIdCode}")
    public ResponseEntity<ApiResponse<List<RequirementAuditResponse>>> findOneByProjectIdAndReqIdCode(@PathVariable("projectId") long projectId, @PathVariable("reqIdCode") String reqIdCode) {
        List<RequirementAuditResponse> responses = requirementAuditService.findByProjectIdAndReqIdCode(projectId, reqIdCode);

        return ResponseEntity.ok()
                .body(ApiResponse.success(responses));
    }

    @GetMapping("/projects/{projectId}/all")
    public ResponseEntity<ApiResponse<List<MatrixResponse>>> findMatrix(@PathVariable("projectId") long projectId) {
        List<MatrixResponse> response = requirementAuditService.createMatrix(projectId);

        return ResponseEntity.ok()
                .body(ApiResponse.success(response));
    }

    @GetMapping("/projects/{projectId}/downloads")
    public ResponseEntity<ByteArrayResource> downloadMatrix(@PathVariable("projectId") long projectId) {
        try {
            List<MatrixResponse> matrixList = requirementAuditService.createMatrix(projectId);
            byte[] excelBytes = exelExportService.generateMatrixExcelFile(matrixList); // 메서드 직접 구현

            String fileName = String.format("%s.xlsx", "Requirement-Matrix");
            ByteArrayResource resource = new ByteArrayResource(excelBytes);

            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(resource);

        } catch (Exception e) {
            log.error("Matrix Excel 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
