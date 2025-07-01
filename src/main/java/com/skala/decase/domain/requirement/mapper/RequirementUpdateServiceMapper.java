package com.skala.decase.domain.requirement.mapper;

import com.skala.decase.domain.document.domain.Document;
import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.requirement.controller.dto.request.SrsUpdateRequestDetail;
import com.skala.decase.domain.requirement.controller.dto.request.UpdateSrsAgentRequest;
import com.skala.decase.domain.requirement.domain.Difficulty;
import com.skala.decase.domain.requirement.domain.Priority;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.domain.RequirementType;
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
    public Requirement toCreateREQEntity(SrsUpdateRequestDetail response, Member member, Project project,
                                         LocalDateTime now,
                                         int revisionCount) {

        Requirement newReq = new Requirement();

        newReq.createUpdateRequirement(
                response.requirement_id(),
                revisionCount,
                response.modified_reason(),
                RequirementType.fromKorean(response.type()),
                response.level1(),
                response.level2(),
                response.level3(),
                response.requirement_name(),
                response.description(),
                Priority.fromEnglish(response.importance()),
                Difficulty.fromEnglish(response.difficulty()),
                now,
                project,
                member
        );
        return newReq;
    }

    /**
     * 요구사항 수정 요청 시 사용되는 매퍼
     */
    public UpdateSrsAgentRequest toUpdateREQ(Requirement requirement) {

        return new UpdateSrsAgentRequest(
                requirement.getReqIdCode(),
                RequirementType.toKorean(requirement.getType().name()),
                requirement.getLevel1(),
                requirement.getLevel2(),
                requirement.getLevel3(),
                requirement.getPriority().name(),
                requirement.getDifficulty().name(),
                requirement.getName(),
                requirement.getDescription()
        );
    }

    public Source toSrcEntity(SrsUpdateRequestDetail response, Requirement requirement, Document document) {

        Source newReq = new Source();

        newReq.createSource(
                requirement,
                document,
                0,
                ""
        );
        return newReq;
    }


}