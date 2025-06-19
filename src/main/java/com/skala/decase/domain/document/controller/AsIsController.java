package com.skala.decase.domain.document.controller;

import com.skala.decase.domain.document.controller.dto.DocumentDetailResponse;
import com.skala.decase.domain.document.service.AsisService;
import com.skala.decase.global.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AS-IS API", description = "as-is 문서 관리를 위한 api 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class AsIsController {

    private final AsisService asisService;
    private final DocumentPreviewController documentPreviewService;

    /**
     * 프로젝트 id에 해당하는 as-is 보고서 목록 조회
     */
    @Operation(summary = "as-is 보고서 목록 조회", description = "as-is 보고서의 목록을 조회합니다.")
    @GetMapping("/{projectId}/documents/as-is")
    public ResponseEntity<ApiResponse<List<DocumentDetailResponse>>> getAsIs(
            @PathVariable("projectId") Long projectId) {
        List<DocumentDetailResponse> responses = asisService.getAsisDocumentList(projectId);
        return ResponseEntity.ok().body(ApiResponse.success(responses));
    }

    /**
     * 특정 as-is 보고서 미리보기
     */
    @Operation(summary = "as-is 보고서 미리보기", description = "as-is보고서의 pdf 미리보기를 지원합니다.")
    @GetMapping("/{projectId}/documents/as-is/{docId}/preview")
    public ResponseEntity<Resource> previewAsis(@PathVariable("projectId") Long projectId,
                                                @PathVariable("docId") String docId) {
        return documentPreviewService.previewDocument(docId);

    }

}