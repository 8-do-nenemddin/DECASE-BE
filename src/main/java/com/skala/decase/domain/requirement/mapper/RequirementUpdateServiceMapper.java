package com.skala.decase.domain.requirement.mapper;

import com.skala.decase.domain.document.domain.Document;
import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.requirement.domain.Difficulty;
import com.skala.decase.domain.requirement.domain.Priority;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.domain.RequirementType;
import com.skala.decase.domain.requirement.service.dto.response.UpdateRfpResponse;
import com.skala.decase.domain.source.domain.Source;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class RequirementUpdateServiceMapper {

    /**
     * 요구사항 처음 생성시 사용되는 매퍼
     */
    public Requirement toCreateREQEntity(UpdateRfpResponse response, Member member, Project project, LocalDateTime now,
                                         int revisionCount) {

        Requirement newReq = new Requirement();

        newReq.createUpdateRequirement(
                response.id(),
                revisionCount,
                response.mod_reason(),
                RequirementType.fromKorean(response.type()),
                response.category_large(),
                response.category_medium(),
                response.category_small(),
                response.name(),
                response.description_content(),
                Priority.fromKorean(response.importance()),
                Difficulty.fromKorean(response.difficulty()),
                now,
                project,
                member
        );
        return newReq;
    }

    /**
     * 요구사항 수정 시 사용되는 매퍼
     */
    public Requirement toUpdateREQEntity(UpdateRfpResponse response, Member member, Project project, LocalDateTime now,
                                         int revisionCount) {

        Requirement newReq = new Requirement();

        newReq.createUpdateRequirement(
                response.id(),
                revisionCount,
                response.mod_reason(),
                RequirementType.fromKorean(response.type()),
                response.category_large(),
                response.category_medium(),
                response.category_small(),
                response.name(),
                response.description_content(),
                Priority.fromKorean(response.importance()),
                Difficulty.fromKorean(response.difficulty()),
                now,
                project,
                member
        );
        return newReq;
    }

    public Source toSrcEntity(UpdateRfpResponse response, Requirement requirement, Document document) {

        Source newReq = new Source();

        newReq.createSource(
                requirement,
                document,
                Integer.parseInt(response.source_pages()),  //int로 변환할까 -> 이미 변환 했는데.. ?
                response.acceptance_criteria()
        );
        return newReq;
    }


}