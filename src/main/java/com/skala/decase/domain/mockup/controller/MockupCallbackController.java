package com.skala.decase.domain.mockup.controller;

import com.skala.decase.domain.mockup.service.CreateMockupService;
import com.skala.decase.global.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Mockup API", description = "목업 관리를 위한 api 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/projects/{projectId}/mockups/callback")
@RequiredArgsConstructor
public class MockupCallbackController {

    private final CreateMockupService createMockupService;

    @PostMapping
    @Operation(summary = "목업 생성 콜백", description = "목업 생성 콜백")
    public ResponseEntity<ApiResponse<String>> handleMockupCallback(
            @RequestParam Long projectId,
            @RequestParam Integer revisionCount,
            @RequestParam("mockUpZip") MultipartFile mockUpZip,
            @RequestParam String status) {
        log.info("FastAPI 콜백 수신: projectId={}, revisionCount={}, status={}", projectId, revisionCount, status);
        createMockupService.saveMockUp(projectId, revisionCount, mockUpZip, status);
        return ResponseEntity.ok().body(ApiResponse.success("목업이 생성되었습니다."));
    }
} 