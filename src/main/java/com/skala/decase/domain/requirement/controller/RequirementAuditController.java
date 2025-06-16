package com.skala.decase.domain.requirement.controller;

import com.skala.decase.domain.requirement.controller.dto.response.MatrixResponse;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementAuditResponse;
import com.skala.decase.domain.requirement.service.RequirementAuditService;
import com.skala.decase.global.model.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "Matrix API", description = "요구사항 추적 매트릭스 관리를 위한 api 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/matrix/")
public class RequirementAuditController {

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
}
