package com.skala.decase.domain.mockup.controller;

import com.skala.decase.domain.mockup.service.CreateMockupService;
import com.skala.decase.domain.mockup.service.CreateScreenSpecService;
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
@RequestMapping("/api/v1/projects/{projectId}")
@RequiredArgsConstructor
public class MockupCallbackController {

    private final CreateMockupService createMockupService;
    private final CreateScreenSpecService createScreenSpecService;

    @PostMapping("/mockups/callback")
    @Operation(summary = "목업 생성 콜백", description = "목업 생성 콜백")
    public ResponseEntity<ApiResponse<String>> handleMockupCallback(
            @RequestParam Long projectId,
            @RequestParam Integer revisionCount,
            @RequestParam("mockUpZip") MultipartFile mockUpZip,
            @RequestParam String status) {
        log.info("FastAPI 콜백 수신: projectId={}, revisionCount={}, status={}, fileSize={}",
                projectId, revisionCount, status, mockUpZip.getSize());

        // 목업 저장
        createMockupService.saveMockUp(projectId, revisionCount, mockUpZip, status);
        log.info("목업 저장 완료: projectId={}, revisionCount={}", projectId, revisionCount);

        // 화면 정의서 생성 요청 (비동기)
        try {
            createScreenSpecService.callFastApiScreenSpecAsync(projectId, revisionCount);
            log.info("화면 정의서 생성 요청 완료: projectId={}, revisionCount={}", projectId, revisionCount);
        } catch (Exception e) {
            log.error("화면 정의서 생성 요청 실패: projectId={}, revisionCount={}, error={}",
                    projectId, revisionCount, e.getMessage(), e);
            // 화면 정의서 생성 요청이 실패해도 목업 콜백은 성공으로 처리
        }

        return ResponseEntity.ok().body(ApiResponse.success("목업이 생성되었습니다."));
    }

    @PostMapping("/screen-spec/callback")
    @Operation(summary = "화면 정의서 생성 콜백", description = "화면 정의서 생성 콜백")
    public ResponseEntity<ApiResponse<String>> handleScreenSpecCallback(
            @RequestParam Long projectId,
            @RequestParam Integer revisionCount,
            @RequestParam String status) {
        log.info("FastAPI 콜백 수신: projectId={}, revisionCount={}, status={}", projectId, revisionCount, status);
        createScreenSpecService.saveScreenSpec(projectId, revisionCount, status);

        return ResponseEntity.ok().body(ApiResponse.success("화면 정의서가 생성되었습니다."));
    }
} 