package com.skala.decase.domain.project.mapper;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.controller.dto.request.CreateProjectRequest;
import com.skala.decase.domain.project.controller.dto.response.*;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.source.domain.Source;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    public ProjectDetailResponseDto toDetailResponse(Project project, Member creator) {
        return new ProjectDetailResponseDto(
                project.getProjectId(),
                project.getName(),
                project.getScale(),
                project.getStartDate(),
                project.getEndDate(),
                project.getDescription(),
                project.getProposalPM(),
                creator.getMemberId()
        );
    }

    public DocumentResponse toMappingDocs(Source source) {
        return new DocumentResponse(
                source.getDocument().getName(),
                source.getPageNum(),
                source.getRelSentence()
        );
    }

    public MappingTableResponseDto toMappingTable(Requirement requirement, List<DocumentResponse> documentResponse) {
        return new MappingTableResponseDto(
                requirement.getReqIdCode(),
                requirement.getName(),
                requirement.getDescription(),
                documentResponse
        );
    }

    public EditProjectResponseDto toEditResponse(Project project) {
        return new EditProjectResponseDto(
                project.getProjectId(),
                project.getName(),
                project.getScale(),
                project.getStartDate(),
                project.getEndDate(),
                project.getDescription());
    }
}