package com.skala.decase.domain.requirement.mapper;

import com.skala.decase.domain.document.domain.Document;
import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.requirement.controller.dto.request.SourceCallbackReq;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementWithSourceResponse;
import com.skala.decase.domain.requirement.controller.dto.response.SourceResponse;
import com.skala.decase.domain.requirement.domain.Difficulty;
import com.skala.decase.domain.requirement.domain.Priority;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.domain.RequirementType;
import com.skala.decase.domain.requirement.controller.dto.request.CreateRfpRequest;
import com.skala.decase.domain.source.domain.Source;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class RequirementServiceMapper {

    public Requirement toREQEntity(CreateRfpRequest response, Member member, Project project, LocalDateTime now) {

        String description = "[문서 페이지]\n" + response.target_page() + "\n"
                + "[대상 업무]\n" + response.description() + "\n";

        return Requirement.builder()
                .reqIdCode(response.requirement_id())
                .type(RequirementType.fromKorean(response.type()))
                .level1(response.category_large())
                .level2(response.category_medium())
                .level3(response.category_small())
                .description(description)
                .priority(Priority.fromKorean(response.importance()))
                .difficulty(Difficulty.fromKorean(response.difficulty()))
                .createdDate(LocalDateTime.now())
                .project(project)
                .createdBy(member)
                .build();
    }

    public Source toSrcEntity(SourceCallbackReq response, Requirement requirement, Document document) {

        Source newReq = new Source();

        newReq.createSource(
                requirement,
                document,
                response.source_page(),  //int로 변환할까
                response.original_text()
        );
        return newReq;
    }

    public static RequirementWithSourceResponse toReqWithSrcResponse(Requirement requirement, List<String> modReason,
                                                                     int currentRevisionCount) {
        List<SourceResponse> sourceResponses = requirement.getSources().stream()
                .map(RequirementServiceMapper::toSourceResponse)
                .collect(Collectors.toList());

        DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 조회 시점의 revisionCount보다 이후에 삭제된 경우 false로 반환
        boolean isDeletedAtCurrentRevision;
        if (requirement.isDeleted()) {
            // 삭제된 경우
            if (requirement.getDeletedRevision() == 0) {
                // deletedRevision이 0인 경우 - 삭제된 상태로 추가된 요구사항
                isDeletedAtCurrentRevision = true;
            } else {
                // deletedRevision이 현재 조회 리비전보다 작거나 같으면 삭제된 상태
                isDeletedAtCurrentRevision = requirement.getDeletedRevision() <= currentRevisionCount;
            }
        } else {
            // 삭제되지 않은 경우
            isDeletedAtCurrentRevision = false;
        }

        return new RequirementWithSourceResponse(
                requirement.getReqPk(),
                requirement.getReqIdCode(),
                requirement.getRevisionCount(),
                requirement.getType() != null ? requirement.getType().name() : null,
                requirement.getType() != null ? requirement.getType().name() : null, // status = type으로 가정
                requirement.getLevel1(),
                requirement.getLevel2(),
                requirement.getLevel3(),
                requirement.getPriority() != null ? requirement.getPriority().name() : null,
                requirement.getDifficulty() != null ? requirement.getDifficulty().name() : null,
                requirement.getName(),
                requirement.getDescription(),
                requirement.getCreatedDate() != null ? requirement.getCreatedDate().format(DATE_FORMATTER) : null,
                isDeletedAtCurrentRevision,  // 조회 시점의 revisionCount보다 이후에 삭제된 경우 false로 반환
                requirement.getDeletedRevision(),
                modReason,
                sourceResponses
        );
    }

    private static SourceResponse toSourceResponse(Source source) {
        return new SourceResponse(
                source.getSourceId(),
                source.getDocument() != null ? source.getDocument().getDocId() : null,
                source.getPageNum(),
                source.getRelSentence()
        );
    }
}