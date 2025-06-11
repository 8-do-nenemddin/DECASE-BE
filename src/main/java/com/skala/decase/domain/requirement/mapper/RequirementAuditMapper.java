package com.skala.decase.domain.requirement.mapper;

import com.skala.decase.domain.requirement.controller.dto.response.RequirementAuditDTO;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementAuditResponse;
import com.skala.decase.domain.requirement.domain.Requirement;
import lombok.AllArgsConstructor;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;

@Component
@AllArgsConstructor
public class RequirementAuditMapper {

    public RequirementAuditDTO toEntity(Object[] result) {
        Requirement requirement = (Requirement) result[0];
        DefaultRevisionEntity revisionEntity = (DefaultRevisionEntity) result[1];
        RevisionType revisionType = (RevisionType) result[2];

        return new RequirementAuditDTO(
                requirement,
                revisionEntity.getId(),
                revisionEntity.getRevisionDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                revisionType
        );
    }

    public RequirementAuditResponse toResponse(RequirementAuditDTO requirementAuditDTO) {
        return new RequirementAuditResponse(
                requirementAuditDTO.getRevisionNumber(),
                requirementAuditDTO.getRequirement().getReqIdCode(),
                requirementAuditDTO.getRequirement().getName(),
                requirementAuditDTO.getRequirement().getDescription(),
                requirementAuditDTO.getRequirement().getModReason(),
                requirementAuditDTO.getRevisionDate(),
                requirementAuditDTO.getRequirement().getCreatedBy().getId(),
                requirementAuditDTO.getRequirement().getCreatedBy().getName(),
                requirementAuditDTO.getRevisionType().toString()
        );
    }
}
