package com.skala.decase.domain.requirement.repository;

import com.skala.decase.domain.requirement.controller.dto.response.*;
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<RequirementModReasonResponse> findModReasonByProjectIdAndReqIdCodesNative(Long projectId, List<String> reqIdCodes, Integer revision) {
        String sql =
                "SELECT r.req_id_code, r.mod_reason, rev.revtstmp " +
                        "FROM td_requirements_aud r " +
                        "JOIN revinfo rev ON r.rev = rev.rev " +
                        "WHERE r.project_id_aud = :projectId " +
                        "AND r.req_id_code IN (:reqIdCodes) " +
                        "AND r.revision_count <= :targetRevision " +
                        "ORDER BY r.req_id_code";

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter("projectId", projectId)
                .setParameter("reqIdCodes", reqIdCodes)
                .setParameter("targetRevision", revision)
                .getResultList();

        return results.stream()
                .map(requirementAuditMapper::toModReasonResponse)
                .toList();
    }

    public List<RequirementResponse> findByProjectIdAndRevisionCount(long projectId, int revisionCount) {
        String sql =
                "WITH target_requirements AS ( " +
                        "  SELECT * " +
                        "  FROM ( " +
                        "    SELECT *, ROW_NUMBER() OVER (PARTITION BY req_id_code ORDER BY modified_date DESC) AS rn " +
                        "    FROM td_requirements_aud " +
                        "    WHERE revision_count <= :targetRevision " +
                        "  ) sub_r " +
                        "  WHERE rn = 1 " +
                        "), " +
                        "distinct_sources AS ( " +
                        "  SELECT * " +
                        "  FROM ( " +
                        "    SELECT *, ROW_NUMBER() OVER (PARTITION BY req_id_code, doc_id ORDER BY revision_count DESC) AS rn " +
                        "    FROM td_source_aud " +
                        "    WHERE revision_count <= :targetRevision " +
                        "  ) sub_s " +
                        "  WHERE rn = 1 " +
                        ") " +
                        "SELECT " +
                        "  r.req_pk, r.req_id_code, " +
                        "  r.type, r.level_1, r.level_2, r.level_3, " +
                        "  r.name, r.description, r.priority, r.difficulty, " +
                        "  r.modified_date AS modified_date, " +
                        "  r.created_date AS created_date, " +
                        "  r.revtype, r.reception, " +
                        "  IFNULL(JSON_ARRAYAGG(DISTINCT " +
                        "    JSON_OBJECT( " +
                        "      'source_id', s.source_id, " +
                        "      'page_num', s.page_num, " +
                        "      'rel_sentence', s.rel_sentence, " +
                        "      'doc_id', s.doc_id, " +
                        "      'doc_name', CASE " +
                        "                    WHEN s.doc_id LIKE 'RFP%' THEN 'RFP' " +
                        "                    ELSE IFNULL(d.name, '') " +
                        "                 END " +
                        "    ) " +
                        "  ), JSON_ARRAY()) AS sources " +
                        "FROM target_requirements r " +
                        "LEFT JOIN distinct_sources s ON r.req_id_code = s.req_id_code " +
                        "LEFT JOIN tm_documents_aud d ON s.doc_id = d.doc_id " +
                        "WHERE r.revtype <> 2 " +
                        "  AND r.project_id_aud = :projectId " +
                        "GROUP BY " +
                        "  r.req_pk, r.req_id_code, " +
                        "  r.type, r.level_1, r.level_2, r.level_3, " +
                        "  r.name, r.description, r.priority, r.difficulty, " +
                        "  r.modified_date, r.created_date, r.revtype, r.reception " +
                        "ORDER BY r.req_id_code";

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter("projectId", projectId)
                .setParameter("targetRevision", revisionCount)
                .getResultList();

        return results.stream()
                .map(result -> requirementAuditMapper.toDtoResponse(result, revisionCount))
                .toList();
    }
}
