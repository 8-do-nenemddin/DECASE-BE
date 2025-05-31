package com.skala.decase.domain.project.controller;

import com.skala.decase.domain.project.controller.dto.request.CreateProjectRequest;
import com.skala.decase.domain.project.controller.dto.response.EditProjectResponseDto;
import com.skala.decase.domain.project.controller.dto.response.ProjectResponse;
import com.skala.decase.domain.project.domain.ProjectApiDocument;
import com.skala.decase.domain.project.service.ProjectService;
import com.skala.decase.global.model.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Project API", description = "프로젝트 관리를 위한 api 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 프로젝트 생성
     *
     * @param request
     * @return
     */
    @PostMapping("")
    @ProjectApiDocument.CreateApiDoc
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.ok().body(ApiResponse.created(response));
    }

    // 프로젝트 수정
    @PutMapping("/{projectId}")
    public ResponseEntity<EditProjectResponseDto> updateProject(
            @PathVariable Long projectId,
            @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(projectService.editProject(projectId, request));
    }

    // 프로젝트 삭제
    @DeleteMapping("{projectId}")
    public ResponseEntity<String> deleteProject(
            @PathVariable Long projectId){
        projectService.deleteProject(projectId);
        return ResponseEntity.ok("프로젝트가 삭제되었습니다.");
    }
}
