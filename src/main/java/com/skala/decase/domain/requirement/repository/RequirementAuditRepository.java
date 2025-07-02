package com.skala.decase.domain.requirement.repository;

import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.exception.ProjectException;
import com.skala.decase.domain.project.service.ProjectService;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementAuditDTO;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementAuditResponse;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementModReasonResponse;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.exception.HistoryException;
import com.skala.decase.domain.requirement.mapper.RequirementAuditMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import java.util.Comparator;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RequirementAuditRepository {

    private final EntityManager entityManager;
    private final RequirementAuditMapper requirementAuditMapper;

    public List<RequirementAuditDTO> getRequirementHistoryByProjectId(long projectId) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> results = auditReader.createQuery()
                .forRevisionsOfEntity(Requirement.class, false, true) // entity, revisionEntity, revisionType
                .add(AuditEntity.property("projectIdAud").eq(projectId))
                .getResultList();

        if (results.isEmpty()) {
            throw new HistoryException("해당 프로젝트에 대한 히스토리가 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        return results.stream()
                .map(requirementAuditMapper::toEntity)
                .sorted(Comparator.comparing(RequirementAuditDTO::getRevisionDate).reversed())
                .toList();
    }

    public List<RequirementAuditDTO> getRequirementHistoryByProjectIdAndReqIdCode(long projectId, String reqIdCode) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> results = auditReader.createQuery()
                .forRevisionsOfEntity(Requirement.class, false, true)
                .add(AuditEntity.property("projectIdAud").eq(projectId))
                .add(AuditEntity.property("reqIdCode").eq(reqIdCode))
                .getResultList();

        if (results.isEmpty()) {
            throw new HistoryException("해당 요구사항에 대한 히스토리가 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        return results.stream()
                .map(requirementAuditMapper::toEntity)
                .sorted(Comparator.comparing(RequirementAuditDTO::getRevisionDate).reversed())
                .toList();
    }

    public List<RequirementModReasonResponse> findModReasonByProjectIdAndReqIdCodesNative(Long projectId, List<String> reqIdCodes) {
        String sql = "SELECT r.req_id_code, r.mod_reason, rev.revtstmp " +
                "FROM td_requirements_aud r " +
                "JOIN revinfo rev ON r.rev = rev.rev " +
                "WHERE r.project_id_aud = :projectId " +
                "AND r.req_id_code IN :reqIdCodes " +
                "ORDER BY r.req_id_code";

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter("projectId", projectId)
                .setParameter("reqIdCodes", reqIdCodes)
                .getResultList();

        return results.stream()
                .map(requirementAuditMapper::toModReasonResponse)
                .toList();
    }
}
