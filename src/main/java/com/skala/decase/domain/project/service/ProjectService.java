package com.skala.decase.domain.project.service;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.member.service.MemberService;
import com.skala.decase.domain.project.controller.dto.request.CreateProjectRequest;
import com.skala.decase.domain.project.controller.dto.response.EditProjectResponseDto;
import com.skala.decase.domain.project.controller.dto.response.ProjectResponse;
import com.skala.decase.domain.project.domain.MemberProject;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.exception.ProjectException;
import com.skala.decase.domain.project.mapper.MemberProjectMapper;
import com.skala.decase.domain.project.mapper.ProjectMapper;
import com.skala.decase.domain.member.repository.MemberProjectRepository;
import com.skala.decase.domain.project.repository.ProjectRepository;
import java.time.LocalDateTime;
import java.util.List;

import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.repository.RequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final MemberProjectRepository memberProjectRepository;

    private final MemberService memberService;

    private final ProjectMapper projectMapper;
    private final MemberProjectMapper memberProjectMapper;

    /**
     * 프로젝트 존재 확인
     */
    public Project findByProjectId(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectException("존재하지 않는 프로젝트입니다.", HttpStatus.NOT_FOUND));

    }

    /**
     * 프로젝트 생성
     *
     * @param request
     * @return
     */
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        validateProjectCreateRequest(request);

        //프로젝트 생성한 사용자 찾기
        Member creator = memberService.findByMemberId(request.creatorMemberId());
        //프로젝트 생성
        LocalDateTime now = LocalDateTime.now();
        Project project = projectMapper.toInitialEntity(request, now);
        Project savedProject = projectRepository.save(project);

        //프로젝트-생성한 사용자 관리
        MemberProject memberProject = memberProjectMapper.toAdminEntity(creator, savedProject);
        memberProjectRepository.save(memberProject);

        return projectMapper.toResponse(savedProject, creator);
    }

    /**
     * 프로젝트 생성 유효성 검증
     *
     * @param request
     */
    private void validateProjectCreateRequest(CreateProjectRequest request) {
        // 종료일이 시작일보다 이후인지 검증
        if (request.endDate().before(request.startDate())) {
            throw new ProjectException("종료일은 시작일 이후여야 합니다.", HttpStatus.BAD_REQUEST);
        }

        // 프로젝트 규모 검증 (음수 불가)
        if (request.scale() < 0) {
            throw new ProjectException("프로젝트 규모는 0 이상이어야 합니다.", HttpStatus.BAD_REQUEST);
        }
    }

    // 프로젝트 수정
    @Transactional
    public EditProjectResponseDto editProject(Long projectId, CreateProjectRequest request) {
        // 프로젝트 존재 확인
        Project editProject = findByProjectId(projectId);
        // 유효성 검증
        validateProjectCreateRequest(request);

        editProject.setName(request.name());
        editProject.setProposalPM(request.proposalPM());
        editProject.setDescription(request.description());
        editProject.setScale(request.scale());
        editProject.setStartDate(request.startDate());
        editProject.setEndDate(request.endDate());
        editProject.setModifiedDate(LocalDateTime.now()); // 수정 시각 갱신

        Project saved = projectRepository.save(editProject);
        return EditProjectResponseDto.fromEntity(saved);
    }

    @Transactional
    public String deleteProject(Long projectId) {
        Project project = findByProjectId(projectId);
        projectRepository.delete(project);
        return "프로젝트가 삭제되었습니다.";
    }
}
