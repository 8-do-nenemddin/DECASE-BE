package com.skala.decase.domain.project.service;

import com.skala.decase.domain.job.domain.Job;
import com.skala.decase.domain.job.repository.JobRepository;
import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.member.repository.MemberProjectRepository;
import com.skala.decase.domain.member.service.MemberService;
import com.skala.decase.domain.project.controller.dto.request.CreateProjectRequest;
import com.skala.decase.domain.project.controller.dto.response.*;
import com.skala.decase.domain.project.domain.MemberProject;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.domain.ProjectInvitation;
import com.skala.decase.domain.project.exception.ProjectException;
import com.skala.decase.domain.project.mapper.MemberProjectMapper;
import com.skala.decase.domain.project.mapper.ProjectMapper;
import com.skala.decase.domain.project.mapper.SuccessMapper;
import com.skala.decase.domain.project.repository.ProjectInvitationRepository;
import com.skala.decase.domain.project.repository.ProjectRepository;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.repository.RequirementRepository;
import com.skala.decase.domain.source.domain.Source;
import com.skala.decase.domain.source.service.SourceRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final MemberProjectRepository memberProjectRepository;
    private final SourceRepository sourceRepository;
    private final MemberProjectRepository memberProjectInvitationRepository;

    private final MemberService memberService;

    private final ProjectMapper projectMapper;
    private final MemberProjectMapper memberProjectMapper;
    private final SuccessMapper successMapper;
    private final RequirementRepository requirementRepository;

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
        return projectMapper.toEditResponse(saved);
    }

    @Transactional
    public DeleteProjectResponse deleteProject(Long projectId) {
        Project project = findByProjectId(projectId);
        project.delete();
        projectRepository.save(project);
        return successMapper.toDelete();
    }

    // 단일 프로젝트 상세 설명
    public ProjectDetailResponseDto getProject(Long projectId) {
        Project project = findByProjectId(projectId);
        MemberProject memberProject = memberProjectRepository.findByProjectId(projectId)
                .stream().filter(MemberProject::isAdmin).toList().get(0);
        Member creator = memberService.findByMemberId(memberProject.getMember().getMemberId());
        project.setRevisionCount(requirementRepository.getMaxRevisionCount(project) == null ? 0 : requirementRepository.getMaxRevisionCount(project));
        return projectMapper.toDetailResponse(project, creator);
    }

    // 조견표 리스트 생성
    public List<MappingTableResponseDto> createMappingTable(Long projectId) {
        Project project = findByProjectId(projectId);
        List<Requirement> requirements = project.getRequirements();

        List<MappingTableResponseDto> result = new ArrayList<>();

        for (Requirement requirement : requirements) {
            List<Source> sources = sourceRepository.findAllByRequirement(requirement);

            List<DocumentResponse> responseDtos = new ArrayList<>();

            if (sources.isEmpty()) {
                responseDtos.add(projectMapper.toMappingDocs(null));
                continue;
            }
            for (Source source : sources) {
                responseDtos.add(projectMapper.toMappingDocs(source));
            }

            result.add(projectMapper.toMappingTable(requirement, responseDtos));
        }
        return result.stream().sorted(Comparator.comparing(MappingTableResponseDto::req_code)).toList();
    }

    public PermissionResponse getAuthority(Long projectId, Long memberId) {
        Member member = memberService.findByMemberId(memberId);
        MemberProject memberProject = memberProjectRepository.findByProject_ProjectIdAndMember_MemberId(projectId, memberId);
        if (memberProject == null) {
            throw new ProjectException("해당 멤버의 프로젝트 권한을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        Boolean isAdmin = memberProjectRepository.existsByMemberAndIsAdminTrue(member);
        return new PermissionResponse(memberProject.getPermission().toString(), isAdmin);
    }

    public List<RequirementDescriptionResponse> getDescriptions(Long projectId, List<String> ids) {
        return requirementRepository.findByProject_ProjectIdAndReqIdCodeIn(projectId, ids)
                .stream()
                .map(req -> new RequirementDescriptionResponse(req.getReqIdCode(), req.getDescription()))
                .collect(Collectors.toList());
    }

    public void save(Project project) {
        projectRepository.save(project);
    }
}
