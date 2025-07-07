package com.skala.decase.domain.requirement.repository;

import com.skala.decase.domain.requirement.controller.dto.response.RequirementAuditDTO;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementModReasonResponse;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementResponse;
import com.skala.decase.domain.requirement.controller.dto.response.SourceResponse;
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

import java.util.*;
import java.util.function.Function;
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
        String requirementsQuery =
                "SELECT " +
                        "  r.req_pk, r.req_id_code, " +
                        "  r.type, r.level_1, r.level_2, r.level_3, " +
                        "  r.name, r.description, r.priority, r.difficulty, " +
                        "  r.modified_date AS modified_date, " +
                        "  r.created_date AS created_date, " +
                        "  r.revtype, r.reception " +
                        "FROM ( " +
                        "  SELECT *, ROW_NUMBER() OVER (PARTITION BY req_id_code ORDER BY modified_date DESC) AS rn " +
                        "  FROM td_requirements_aud " +
                        "  WHERE revision_count <= :targetRevision " +
                        "    AND project_id_aud = :projectId " +
                        "    AND revtype <> 2 " +
                        ") r " +
                        "WHERE r.rn = 1 " +
                        "ORDER BY r.req_id_code";

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(requirementsQuery)
                .setParameter("projectId", projectId)
                .setParameter("targetRevision", revisionCount)
                .getResultList();

        // 첫 번째 쿼리 결과에서 req_pk 리스트 추출
        List<Long> reqPkList = results.stream()
                .map(result -> ((Number) result[0]).longValue())
                .collect(Collectors.toList());

        // req_pk 리스트가 비어있으면 빈 결과 반환
        if (reqPkList.isEmpty()) {
            return Collections.emptyList();
        }

        String sourcesQuery =
                "SELECT " +
                        "  s.req_id_code, " +
                        "  s.source_id, " +
                        "  s.page_num, " +
                        "  s.rel_sentence, " +
                        "  s.doc_id, " +
                        "  CASE " +
                        "    WHEN s.doc_id LIKE '%RFP%' THEN 'RFP' " +
                        "    ELSE IFNULL(d.name, '') " +
                        "  END AS doc_name " +
                        "FROM ( " +
                        "  SELECT *, ROW_NUMBER() OVER (PARTITION BY req_id_code, doc_id ORDER BY revision_count DESC) AS rn " +
                        "  FROM td_source_aud " +
                        "  WHERE revision_count <= :targetRevision " +
                        "    AND req_pk IN (:reqPkList) " +
                        ") s " +
                        "LEFT JOIN tm_documents_aud d ON s.doc_id = d.doc_id " +
                        "WHERE s.rn = 1 " +
                        "ORDER BY s.req_id_code, s.doc_id";

        @SuppressWarnings("unchecked")
        List<Object[]> docs = entityManager.createNativeQuery(sourcesQuery)
                .setParameter("targetRevision", revisionCount)
                .setParameter("reqPkList", reqPkList)
                .getResultList();

        // docs를 Map<req_id_code, List<SourceResponse>>로 변환하면서 중복 문서 제거
        Map<String, List<SourceResponse>> sourcesMap = new HashMap<>();

        for (Object[] doc : docs) {
            String reqIdCode = (String) doc[0];
            String docId = (String) doc[4];

            SourceResponse source = new SourceResponse(
                    ((Number) doc[1]).longValue(), // source_id
                    docId,                         // doc_id
                    (String) doc[5],               // doc_name
                    ((Number) doc[2]).intValue(),  // page_num
                    (String) doc[3]                // rel_sentence
            );

            sourcesMap.computeIfAbsent(reqIdCode, k -> new ArrayList<>());

            // 같은 요구사항에서 동일한 doc_id가 이미 있는지 확인
            boolean docExists = sourcesMap.get(reqIdCode).stream()
                    .anyMatch(existingSource -> docId.equals(existingSource.docId()));

            if (!docExists) {
                sourcesMap.get(reqIdCode).add(source);
            }
        }

        return results.stream()
                .map(result -> {
                    RequirementResponse response = requirementAuditMapper.toDtoResponse(result, revisionCount);
                    // 해당 요구사항의 소스 목록을 추가
                    String reqIdCode = (String) result[1]; // req_id_code
                    List<SourceResponse> sources = sourcesMap.getOrDefault(reqIdCode, Collections.emptyList());
                    response.setSources(sources);
                    return response;
                })
                .toList();
    }

    public List<RequirementResponse> findByProjectIdAndRevisionCountToMockup(long projectId, int revisionCount) {
        System.out.println("=== 디버깅 시작 ===");
        System.out.println("projectId: " + projectId + ", revisionCount: " + revisionCount);

        String requirementsQuery =
                "SELECT " +
                        "  r.req_pk, r.req_id_code, " +
                        "  r.type, r.level_1, r.level_2, r.level_3, " +
                        "  r.name, r.description, r.priority, r.difficulty, " +
                        "  r.modified_date AS modified_date, " +
                        "  r.created_date AS created_date, " +
                        "  r.revtype, r.reception " +
                        "FROM ( " +
                        "  SELECT *, ROW_NUMBER() OVER (PARTITION BY req_id_code ORDER BY modified_date DESC) AS rn " +
                        "  FROM td_requirements_aud " +
                        "  WHERE revision_count <= :targetRevision " +
                        "    AND project_id_aud = :projectId " +
                        "    AND revtype <> 2 " +
                        ") r " +
                        "WHERE r.rn = 1 " +
                        "ORDER BY r.req_id_code";

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(requirementsQuery)
                .setParameter("projectId", projectId)
                .setParameter("targetRevision", revisionCount)
                .getResultList();

        System.out.println("전체 쿼리 결과 수: " + results.size());

        // 타입별 분포 확인
        Map<String, Long> typeCount = results.stream()
                .collect(Collectors.groupingBy(
                        result -> (String) result[2], // type 컬럼
                        Collectors.counting()
                ));

        System.out.println("타입별 분포: " + typeCount);

        // 처음 몇 개 결과 출력
        for (int i = 0; i < Math.min(5, results.size()); i++) {
            Object[] result = results.get(i);
            System.out.println("결과 " + i + ": req_pk=" + result[0] +
                    ", req_id_code=" + result[1] +
                    ", type=" + result[2] +
                    ", name=" + result[6]);
        }

        // Java에서 FR 타입만 필터링
        List<Object[]> frResults = results.stream()
                .filter(result -> {
                    String type = (String) result[2];
                    System.out.println("타입 체크: " + type + " -> " + "FR".equals(type));
                    return "FR".equals(type);
                })
                .collect(Collectors.toList());

        System.out.println("FR 타입 필터링 후 결과 수: " + frResults.size());

        // FR 타입 결과에서 req_pk 리스트 추출
        List<Long> reqPkList = frResults.stream()
                .map(result -> ((Number) result[0]).longValue())
                .collect(Collectors.toList());

        System.out.println("reqPkList: " + reqPkList);

        // req_pk 리스트가 비어있으면 빈 결과 반환
        if (reqPkList.isEmpty()) {
            System.out.println("reqPkList가 비어있음 - 빈 결과 반환");
            return Collections.emptyList();
        }

        String sourcesQuery =
                "SELECT " +
                        "  s.req_id_code, " +
                        "  s.source_id, " +
                        "  s.page_num, " +
                        "  s.rel_sentence, " +
                        "  s.doc_id, " +
                        "  CASE " +
                        "    WHEN s.doc_id LIKE '%RFP%' THEN 'RFP' " +
                        "    ELSE IFNULL(d.name, '') " +
                        "  END AS doc_name " +
                        "FROM ( " +
                        "  SELECT *, ROW_NUMBER() OVER (PARTITION BY req_id_code, doc_id ORDER BY revision_count DESC) AS rn " +
                        "  FROM td_source_aud " +
                        "  WHERE revision_count <= :targetRevision " +
                        "    AND req_pk IN (:reqPkList) " +
                        ") s " +
                        "LEFT JOIN tm_documents_aud d ON s.doc_id = d.doc_id " +
                        "WHERE s.rn = 1 " +
                        "ORDER BY s.req_id_code, s.doc_id";

        @SuppressWarnings("unchecked")
        List<Object[]> docs = entityManager.createNativeQuery(sourcesQuery)
                .setParameter("targetRevision", revisionCount)
                .setParameter("reqPkList", reqPkList)
                .getResultList();

        System.out.println("소스 쿼리 결과 수: " + docs.size());

        // docs를 Map<req_id_code, List<SourceResponse>>로 변환하면서 중복 문서 제거
        Map<String, List<SourceResponse>> sourcesMap = new HashMap<>();

        for (Object[] doc : docs) {
            String reqIdCode = (String) doc[0];
            String docId = (String) doc[4];

            SourceResponse source = new SourceResponse(
                    ((Number) doc[1]).longValue(), // source_id
                    docId,                         // doc_id
                    (String) doc[5],               // doc_name
                    ((Number) doc[2]).intValue(),  // page_num
                    (String) doc[3]                // rel_sentence
            );

            sourcesMap.computeIfAbsent(reqIdCode, k -> new ArrayList<>());

            // 같은 요구사항에서 동일한 doc_id가 이미 있는지 확인
            boolean docExists = sourcesMap.get(reqIdCode).stream()
                    .anyMatch(existingSource -> docId.equals(existingSource.docId()));

            if (!docExists) {
                sourcesMap.get(reqIdCode).add(source);
            }
        }

        // FR 타입 결과만 사용해서 최종 응답 생성
        List<RequirementResponse> finalResult = frResults.stream()
                .map(result -> {
                    RequirementResponse response = requirementAuditMapper.toDtoResponse(result, revisionCount);
                    // 해당 요구사항의 소스 목록을 추가
                    String reqIdCode = (String) result[1]; // req_id_code
                    List<SourceResponse> sources = sourcesMap.getOrDefault(reqIdCode, Collections.emptyList());
                    response.setSources(sources);
                    return response;
                })
                .toList();

        System.out.println("최종 결과 수: " + finalResult.size());
        System.out.println("=== 디버깅 끝 ===");

        return finalResult;
    }

    public List<RequirementResponse> findByProjectIdAndRevisionCountToUpdate(Long projectId, int revisionCount) {
        String requirementsQuery =
                "SELECT " +
                        "  r.req_pk, r.req_id_code, " +
                        "  r.type, r.level_1, r.level_2, r.level_3, " +
                        "  r.name, r.description, r.priority, r.difficulty, " +
                        "  r.modified_date AS modified_date, " +
                        "  r.created_date AS created_date, " +
                        "  r.revtype, r.reception " +
                        "FROM ( " +
                        "  SELECT *, ROW_NUMBER() OVER (PARTITION BY req_id_code ORDER BY modified_date DESC) AS rn " +
                        "  FROM td_requirements_aud " +
                        "  WHERE revision_count <= :targetRevision " +
                        "    AND project_id_aud = :projectId " +
                        "    AND revtype <> 2 " +
                        ") r " +
                        "WHERE r.rn = 1 " +
                        "ORDER BY r.req_id_code";

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(requirementsQuery)
                .setParameter("projectId", projectId)
                .setParameter("targetRevision", revisionCount)
                .getResultList();

        return results.stream()
                .map(result -> {
                    RequirementResponse response = requirementAuditMapper.toDtoResponse(result, revisionCount);
                    response.setSources(null);
                    return response;
                })
                .toList();
    }
}
