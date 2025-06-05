package com.skala.decase.domain.requirement.controller;

import com.skala.decase.domain.requirement.controller.dto.RequirementDto;
import com.skala.decase.domain.requirement.controller.dto.RequirementRevisionDto;
import com.skala.decase.domain.requirement.controller.dto.UpdateRequirementDto;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementWithSourceResponse;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.service.RequirementService;
import com.skala.decase.global.model.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Requirement API", description = "요구사항 관리를 위한 api 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/")
public class RequirementController {

    private final RequirementService repositoryService;

    // 프로젝트의 요구사항 정의서 리스트 버전별 불러오기
    @GetMapping("/{projectId}/requirements/generated")
    public ResponseEntity<ApiResponse<List<RequirementWithSourceResponse>>> getGeneratedRequirements(
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer revisionCount) {

        List<RequirementWithSourceResponse> responses = (revisionCount == null)
                ? repositoryService.getGeneratedRequirements(projectId)
                : repositoryService.getGeneratedRequirements(projectId, revisionCount);

        return ResponseEntity.ok().body(ApiResponse.success(responses));
    }

    // 프로젝트의 요구사항 분류(대/중/소) 불러오기
    @GetMapping("/{projectId}/documents/categories")
    public ResponseEntity<Map<String, List<String>>> getRequirementCategory(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(repositoryService.getRequirementCategory(projectId));
    }

    // 프로젝트의 쿼리 및 카테고리 별 검색
    @GetMapping("/{projectId}/documents/search")
    public ResponseEntity<List<RequirementDto>> getGeneratedRequirements(
            @PathVariable Long projectId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String level1,
            @RequestParam(required = false) String level2,
            @RequestParam(required = false) String level3,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) List<String> docType) {
        List<RequirementDto> result = repositoryService.getFilteredRequirements(
                projectId, query, level1, level2, level3, type, difficulty, priority, docType);

        return ResponseEntity.ok(result);
    }


    @GetMapping("/{projectId}/revision")
    public ResponseEntity<List<RequirementRevisionDto>> getRequirementVersion(
            @PathVariable Long projectId) {
        List<RequirementRevisionDto> revisions = repositoryService.getRequirementRevisions(projectId);
        return ResponseEntity.ok(revisions);
    }

    @PostMapping("{projectId}/requirements/edit")
    public ResponseEntity<RequirementDto> updateRequirement(
            @PathVariable Long projectId,
            @RequestBody UpdateRequirementDto dto) {
        Requirement updatedReq = repositoryService.updateRequirement(projectId, dto);
        RequirementDto rd = RequirementDto.fromEntity(updatedReq);
        return ResponseEntity.ok(rd);
    }

    @PatchMapping("{projectId}/requirments/{reqPk}/delete")
    public String deleteRequirement(
            @PathVariable Long projectId,
            @PathVariable Long reqPk) {
        return repositoryService.deleteRequirement(projectId, reqPk);
    }
}
