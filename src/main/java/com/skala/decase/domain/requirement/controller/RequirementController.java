package com.skala.decase.domain.requirement.controller;

import com.skala.decase.domain.requirement.controller.dto.request.DeleteRequestDto;
import com.skala.decase.domain.requirement.controller.dto.request.RequirementRevisionDto;
import com.skala.decase.domain.requirement.controller.dto.request.UpdateRequirementDto;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementWithSourceResponse;
import com.skala.decase.domain.requirement.service.ExelExportService;
import com.skala.decase.domain.requirement.service.RequirementService;
import com.skala.decase.global.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Requirement API", description = "요구사항 관리를 위한 api 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/")
public class RequirementController {

    private final RequirementService requirementService;
    private final ExelExportService exelExportService;

    @Operation(summary = "요구사항 정의서 버전별 미리보기", description = "특정 리비전의 요구사항 정의서 미리보기를 지원합니다.")
    @GetMapping("/{projectId}/requirements/generated")
    public ResponseEntity<ApiResponse<List<RequirementWithSourceResponse>>> getGeneratedRequirements(
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer revisionCount) {

        List<RequirementWithSourceResponse> responses = (revisionCount == null)
                ? requirementService.getGeneratedRequirements(projectId)
                : requirementService.getGeneratedRequirements(projectId, revisionCount);

        return ResponseEntity.ok().body(ApiResponse.success(responses));
    }

    @Operation(summary = "요구사항 정의서 버전별 다운로드", description = "특정 리비전의 요구사항 정의서를 엑셀로 다운로드합니다.")
    @GetMapping("/{projectId}/requirements/downloads")
    public ResponseEntity<ByteArrayResource> downloadGeneratedRequirements(
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer revisionCount) {

        try {
            int revision = (revisionCount == null) ? 1 : revisionCount;

            List<RequirementWithSourceResponse> responses = requirementService.getGeneratedRequirements(projectId,
                    revision);

            // Excel 파일 생성
            byte[] excelBytes = exelExportService.generateExcelFile(responses);
            //파일 이름
            String fileName = String.format("%s-v%d.xlsx", "DECASE-Requirements-Specification", revision);

            ByteArrayResource resource = new ByteArrayResource(excelBytes);

            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFileName)
                    .header(HttpHeaders.CONTENT_TYPE,
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(resource);

        } catch (Exception e) {
            log.error("Excel 다운로드 중 오류 발생: projectId={}, revision={}", projectId, revisionCount, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 프로젝트의 요구사항 분류(대/중/소) 불러오기
    @GetMapping("/{projectId}/documents/{revisionCount}/categories")
    public ResponseEntity<Map<String, List<String>>> getRequirementCategory(
            @PathVariable Long projectId, @PathVariable int revisionCount) {
        return ResponseEntity.ok(requirementService.getRequirementCategory(projectId, revisionCount));
    }

    @Operation(summary = "요구사항 정의서 버전 별 검색", description = "요구사항 정의서 버전 별 검색")
    @GetMapping("/{projectId}/documents/{revisionCount}/search")
    public ResponseEntity<ApiResponse<List<RequirementWithSourceResponse>>> getGeneratedRequirements(
            @PathVariable Long projectId,
            @PathVariable int revisionCount,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String level1,
            @RequestParam(required = false) String level2,
            @RequestParam(required = false) String level3,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) List<String> docType) {
        List<RequirementWithSourceResponse> result = requirementService.getFilteredRequirements(
                projectId, revisionCount, query, level1, level2, level3, type, difficulty, priority, docType);

        return ResponseEntity.ok().body(ApiResponse.success(result));
    }

    @GetMapping("/{projectId}/revision")
    public ResponseEntity<List<RequirementRevisionDto>> getRequirementVersion(
            @PathVariable Long projectId) {
        List<RequirementRevisionDto> revisions = requirementService.getRequirementRevisions(projectId);
        return ResponseEntity.ok(revisions);
    }

    @Operation(summary = "클라이언트에서 요구사항 정의서 내용 수정", description = "특정 리비전의 요구사항 정의서를 클라이언트가 수정할 수 있습니다.")
    @PostMapping("{projectId}/requirements/{revisionCount}/edit")
    public ResponseEntity<ApiResponse<String>> updateRequirement(
            @PathVariable Long projectId,
            @PathVariable int revisionCount,
            @RequestBody List<UpdateRequirementDto> dtoList) {
        requirementService.updateRequirement(projectId, revisionCount, dtoList);
        return ResponseEntity.ok().body(ApiResponse.success("변경 내역이 저장되었습니다."));
    }

    @PatchMapping("{projectId}/requirments/{reqPk}/delete")
    public ResponseEntity<ApiResponse<String>> deleteRequirement(
            @PathVariable Long projectId,
            @PathVariable Long reqPk,
            @RequestBody DeleteRequestDto reasonDto) {
        requirementService.deleteRequirement(projectId, reqPk, reasonDto.getReason(), reasonDto.getMemberId());
        return ResponseEntity.ok().body(ApiResponse.success("변경 내역이 저장되었습니다."));
    }
}
