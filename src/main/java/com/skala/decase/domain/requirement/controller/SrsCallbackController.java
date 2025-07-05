package com.skala.decase.domain.requirement.controller;

import com.skala.decase.domain.project.service.AIMailService;
import com.skala.decase.domain.requirement.controller.dto.request.SrsCallbackRequest;
import com.skala.decase.domain.requirement.controller.dto.request.SrsUpdateCallbackRequest;
import com.skala.decase.domain.requirement.service.SrsProcessingService;
import com.skala.decase.domain.requirement.service.SrsUpdateService;
import com.skala.decase.global.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "SRS Callback API", description = "SRS 비동기 작업 콜백을 위한 API입니다.")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class SrsCallbackController {

    private final SrsProcessingService srsProcessingService;
    private final SrsUpdateService srsUpdateService;
    private final AIMailService aiMailService;

    @PostMapping(value = "/{projectId}/asis/callback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "AS-IS 분석 결과 콜백", description = "AS-IS 분석 완료 후 호출되는 콜백 API")
    public ResponseEntity<ApiResponse<String>> handleAsIsCallback(
            @PathVariable Long projectId,
            @RequestParam("member_id") Long memberId,
            @RequestPart("file") MultipartFile file,
            @RequestParam("filename") String filename,
            @RequestParam("status") String status
    ) {
        log.info("AS-IS 분석 콜백 수신 - 프로젝트 ID: {}, 파일명: {}, 상태: {}", projectId, filename, status);
        srsProcessingService.saveAsIsAnalysis(projectId, memberId, file, status);
        return ResponseEntity.ok().body(ApiResponse.success("AS-IS 분석 결과가 성공적으로 저장되었습니다."));
    }

    @PostMapping(value = "/{projectId}/srs-agent/callback")
    @Operation(summary = "요구사항 정의서 생성 결과 콜백", description = "요구사항 정의서 생성 완료 후 호출되는 콜백 API")
    public ResponseEntity<ApiResponse<String>> handleSRSCallback(
            @PathVariable Long projectId,
            @RequestBody SrsCallbackRequest request
    ) {
        log.info("요구사항 정의서 생성 콜백 수신 - 프로젝트 ID: {}, 상태: {}", projectId, request.status());
        srsProcessingService.saveSRSAnalysis(projectId, request.member_id(), request.document_id(), request.status(),
                request.srs());
        return ResponseEntity.ok().body(ApiResponse.success("요구사항 정의서 생성 결과가 성공적으로 저장되었습니다."));
    }

    @PostMapping(value = "/{projectId}/srs-agent/update/callback")
    @Operation(summary = "요구사항 정의서 업데이트 결과 콜백", description = "요구사항 정의서 업데이트 프로세스 완료 후 호출되는 콜백 API")
    public ResponseEntity<ApiResponse<String>> handleSRSUpdateCallback(
            @PathVariable Long projectId,
            @RequestBody SrsUpdateCallbackRequest callback_data
    ) {
        log.info("요구사항 정의서 업데이트 콜백 수신 - 프로젝트 ID: {}, 상태: {}", projectId, callback_data.status());
        srsUpdateService.saveSRSUpdateAnalysis(projectId, callback_data.memberId(), callback_data.documentId(),
                callback_data.status(),
                callback_data.changes());
        return ResponseEntity.ok().body(ApiResponse.success("요구사항 정의서 생성 결과가 성공적으로 저장되었습니다."));
    }
}
