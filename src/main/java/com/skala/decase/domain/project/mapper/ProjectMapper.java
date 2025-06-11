package com.skala.decase.domain.project.mapper;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.controller.dto.request.CreateProjectRequest;
import com.skala.decase.domain.project.controller.dto.response.MappingTableResponseDto;
import com.skala.decase.domain.project.controller.dto.response.ProjectDetailResponseDto;
import com.skala.decase.domain.project.controller.dto.response.ProjectResponse;
import com.skala.decase.domain.project.domain.Project;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.domain.RequirementDocument;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProjectMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Project toInitialEntity(CreateProjectRequest request, LocalDateTime now) {
        return new Project(
                request.name(),
                request.scale(),
                request.startDate(),
                request.endDate(),
                request.description(),
                request.proposalPM(),
                now,
                now
        );
    }

    public ProjectResponse toResponse(Project project, Member creator) {
        return new ProjectResponse(
                project.getProjectId(),
                project.getName(),
                project.getScale(),
                project.getStartDate(),
                project.getEndDate(),
                project.getDescription(),
                project.getProposalPM(),
                project.getRevisionCount(),
                project.getStatus().name(),
                project.getCreatedDate().format(FORMATTER),
                project.getModifiedDate().format(FORMATTER),
                creator.getMemberId(),
                creator.getName()
        );
    }

    public ProjectDetailResponseDto toDetailResponse(Project project) {
        return new ProjectDetailResponseDto(
                project.getProjectId(),
                project.getName(),
                project.getScale(),
                project.getStartDate(),
                project.getEndDate(),
                project.getDescription(),
                project.getProposalPM()
        );
    }

    public MappingTableResponseDto toMappingTable(Requirement requirement, RequirementDocument requirementDocument) {
        return new MappingTableResponseDto(
                requirement.getReqIdCode(),
                requirement.getName(),
                requirement.getDescription(),
                requirementDocument.getDocument().getName(),
                requirementDocument.getPageNum(),
                requirementDocument.getRelSentence()
        );
    }
}