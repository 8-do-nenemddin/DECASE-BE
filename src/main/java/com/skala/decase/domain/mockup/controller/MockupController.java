package com.skala.decase.domain.mockup.controller;

import com.skala.decase.domain.mockup.domain.dto.MockupExistDto;
import com.skala.decase.domain.mockup.domain.dto.MockupUploadResponse;
import com.skala.decase.domain.mockup.service.CreateMockupService;
import com.skala.decase.domain.mockup.service.MockupService;
import com.skala.decase.global.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "Mockup API", description = "목업 관리를 위한 api 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectId}/mockups")
public class MockupController {

    private final MockupService mockupService;
    private final CreateMockupService createMockupService;

    //TODO: 리비전 버전이 없어도 되나?
    // 사이드바 - 요구사항 리비전에 따른 목업 불러오기
    @Operation(summary = "목업 보기", description = "생성된 목업 리비전-파일명 전송")
    @GetMapping("")
    public ResponseEntity<Map<Integer, List<String>>> getMockups(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(mockupService.getMockupsGroupedByRevision(projectId));
    }

    @Operation(summary = "목업 코드", description = "생성된 목업 코드 전송")
    @GetMapping("/{revisionCount}/{fileName}")
    public ResponseEntity<Resource> getMockupCode(
            @PathVariable Long projectId,
            @PathVariable Integer revisionCount,
            @PathVariable String fileName) {
        return mockupService.getMockupCode(projectId, revisionCount, fileName);
    }

    @Operation(summary = "목업 코드 수정 저장", description = "수정됨 목업 코드 저장")
    @PutMapping("/{revisionCount}/{fileName}")
    public ResponseEntity<Void> saveMockupCode(
            @PathVariable Long projectId,
            @PathVariable Integer revisionCount,
            @PathVariable String fileName,
            @RequestBody Map<String, String> requestBody) {
        String code = requestBody.get("code");
        mockupService.saveMockupCode(projectId, revisionCount, fileName, code);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "목업 다운로드", description = "생성된 목업 코드의 다운로드를 지원 (.zip)")
    @GetMapping("/{revisionCount}/download")
    public ResponseEntity<Resource> downloadMockups(
            @PathVariable Long projectId,
            @PathVariable Integer revisionCount) {
        return mockupService.downloadMockups(projectId, revisionCount);
    }

    @Operation(summary = "[테스트용] 목업 업로드", description = "목업 업로드")
    @PostMapping(path = "/{revisionCount}/upload", consumes = "multipart/form-data")
    public ResponseEntity<List<MockupUploadResponse>> uploadMockups(
            @PathVariable Long projectId,
            @PathVariable Integer revisionCount,
            @RequestPart("files") List<MultipartFile> files
    ) throws Exception {
        return ResponseEntity.ok(mockupService.uploadMockups(projectId, revisionCount, files));
    }

    @Operation(summary = "목업 생성", description = "목업 생성")
    @PostMapping(path = "/{revisionCount}")
    public ResponseEntity<ApiResponse<String>> createMockups(
            @PathVariable Long projectId,
            @PathVariable Integer revisionCount,
            @RequestParam("outputFolderName") String outputFolderName
    ) {
        createMockupService.createMockUpAsync(projectId, revisionCount, outputFolderName);

        return ResponseEntity.ok().body(ApiResponse.success("목업이 생성되었습니다."));
    }

	@Operation(summary = "해당 프로젝트 요구사항 리비전에 목업이 생성되었는지 유무 반환")
	@GetMapping("{revisionCount}/exist")
	public ResponseEntity<MockupExistDto> mockupExists(
			@PathVariable Long projectId,
			@PathVariable Integer revisionCount) {
		return ResponseEntity.ok(mockupService.mockupExists(projectId, revisionCount));
	}
}