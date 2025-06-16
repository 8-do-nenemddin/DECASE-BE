package com.skala.decase.domain.requirement.repository;

import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.requirement.domain.Requirement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequirementRepository extends JpaRepository<Requirement, Long> {
    /**
     * 특정 버전의 유효한 요구사항 정의서 목록 불러오기 deletedRevision이 특정 버전 이하인 정보를 불러옵니다.
     *
     * @return 특정 버전에서 유효한 요구사항 정의서
     */
    @Query(value = """
            SELECT *
            FROM td_requirements r
            WHERE r.project_id = :projectId
              AND (r.req_id_code, r.revision_count) IN (
                  SELECT r2.req_id_code, MAX(r2.revision_count)
                  FROM td_requirements r2
                  WHERE r2.project_id = :projectId
                    AND r2.revision_count <= :revisionCount
                  GROUP BY r2.req_id_code
              )
            """, nativeQuery = true)
    List<Requirement> findValidRequirementsByProjectAndRevision(@Param("projectId") Long projectId,
                                                                @Param("revisionCount") int revisionCount);

    List<Requirement> findByProject_AndIsDeletedFalse(Project project);

    @Query("SELECT MAX(r.revisionCount) FROM Requirement r WHERE r.project = :project")
    Integer getMaxRevisionCount(Project project);

    Optional<Requirement> findFirstByProjectAndRevisionCount(Project project, int revisionCount);

    /**
     * 해당 프로젝트 내에서 가장 큰 revision count가 큰 수를 찾아옴
     *
     * @param project
     * @return
     */
    @Query("SELECT MAX(r.revisionCount) FROM Requirement r WHERE r.project = :project")
    Optional<Integer> findMaxRevisionCountByProject(@Param("project") Project project);

    /**
     * 요구사항 정의서의 REQ id로 요구사항 정의서 내용 찾아오기
     *
     * @param id
     * @return
     */
    @Query("SELECT r FROM Requirement r WHERE r.reqIdCode = :reqIdCode and r.isDeleted=false")
    Optional<Requirement> findByReqIdCodeAndIsDeletedFalse(@Param("reqIdCode") String id);

    @Query(value = """
    SELECT r.*
    FROM td_requirements r
    INNER JOIN (
        SELECT r2.req_id_code
        FROM td_requirements r2
        WHERE r2.req_pk IN :reqPks
    ) target_codes ON r.req_id_code = target_codes.req_id_code
    WHERE r.revision_count <= :revisionCount
    """, nativeQuery = true)
    List<Requirement> findRequirementsByReqPksAndRevision(@Param("reqPks") List<Long> reqPks,
                                                   @Param("revisionCount") int revisionCount);

    @Query("SELECT r FROM Requirement r WHERE r.reqIdCode = :reqIdCode")
    Optional<Requirement> findByReqIdCode(@Param("reqIdCode") String id);

    Optional<Requirement> findByProject_ProjectIdAndReqIdCode(Long projectId, String reqIdCode);
}
