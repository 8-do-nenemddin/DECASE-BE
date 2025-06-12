package com.skala.decase.domain.requirement.controller.dto.response;

import com.skala.decase.domain.requirement.domain.Requirement;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.envers.RevisionType;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RequirementAuditDTO {

    private Requirement requirement;
    private int revisionNumber;
    private LocalDateTime revisionDate;
    private RevisionType revisionType;
}
