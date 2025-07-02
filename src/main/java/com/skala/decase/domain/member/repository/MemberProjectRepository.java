package com.skala.decase.domain.member.repository;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.domain.MemberProject;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.domain.ProjectStatus;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberProjectRepository extends JpaRepository<MemberProject, Long> {

    // 특정 회원이 특정 프로젝트에 관리자 권한이 있는지 확인
    @Query("SELECT COUNT(mp) > 0 FROM MemberProject mp WHERE mp.project=:project AND mp.member = :member AND mp.isAdmin = true")
    boolean existsAdminPermission(Project project, Member member);

    @Query("SELECT mp FROM MemberProject mp WHERE mp.project.projectId = :projectId")
    List<MemberProject> findByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT mp FROM MemberProject mp WHERE mp.member.memberId = :memberId")
    List<MemberProject> findByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT mp FROM MemberProject mp WHERE mp.project.projectId = :projectId AND mp.member.memberId = :memberId")
    MemberProject findByProjectIdAndMemberId(@Param("projectId") Long projectId, @Param("memberId") Long memberId);

    /**
     * 프로젝트 목록 검색시 필터링
     */
    @Query("SELECT mp FROM MemberProject mp " +
            "JOIN FETCH mp.project p " +
            "WHERE mp.member.memberId = :memberId " +
            "AND p.isDeleted = false")  // 이 조건 추가
    Page<MemberProject> findByMemberIdWithProject(@Param("memberId") Long memberId, Pageable pageable);

    // 1. 프로젝트명만 필터
    @Query("SELECT mp FROM MemberProject mp " +
            "JOIN FETCH mp.project p " +
            "WHERE mp.member.memberId = :memberId " +
            "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :projectName, '%')) " +
            "AND p.isDeleted = false")
    Page<MemberProject> findByMemberIdAndProjectName(@Param("memberId") Long memberId,
                                                     @Param("projectName") String projectName,
                                                     Pageable pageable);

    // 2. 상태만 필터
    @Query("SELECT mp FROM MemberProject mp " +
            "JOIN FETCH mp.project p " +
            "WHERE mp.member.memberId = :memberId " +
            "AND p.status = :status " +
            "AND p.isDeleted = false")
    Page<MemberProject> findByMemberIdAndStatus(@Param("memberId") Long memberId,
                                                @Param("status") ProjectStatus status,
                                                Pageable pageable);

    // 3. 제안 PM만 필터
    @Query("SELECT mp FROM MemberProject mp " +
            "JOIN FETCH mp.project p " +
            "WHERE mp.member.memberId = :memberId " +
            "AND LOWER(p.proposalPM) LIKE LOWER(CONCAT('%', :proposalPM, '%')) " +
            "AND p.isDeleted = false")
    Page<MemberProject> findByMemberIdAndProposalPM(@Param("memberId") Long memberId,
                                                    @Param("proposalPM") String proposalPM,
                                                    Pageable pageable);

    // 4. 프로젝트명 + 상태
    @Query("SELECT mp FROM MemberProject mp " +
            "JOIN FETCH mp.project p " +
            "WHERE mp.member.memberId = :memberId " +
            "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :projectName, '%')) " +
            "AND p.status = :status " +
            "AND p.isDeleted = false")
    Page<MemberProject> findByMemberIdAndProjectNameAndStatus(@Param("memberId") Long memberId,
                                                              @Param("projectName") String projectName,
                                                              @Param("status") ProjectStatus status,
                                                              Pageable pageable);

    // 5. 프로젝트명 + 제안 PM
    @Query("SELECT mp FROM MemberProject mp " +
            "JOIN FETCH mp.project p " +
            "WHERE mp.member.memberId = :memberId " +
            "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :projectName, '%')) " +
            "AND LOWER(p.proposalPM) LIKE LOWER(CONCAT('%', :proposalPM, '%')) " +
            "AND p.isDeleted = false")
    Page<MemberProject> findByMemberIdAndProjectNameAndProposalPM(@Param("memberId") Long memberId,
                                                                  @Param("projectName") String projectName,
                                                                  @Param("proposalPM") String proposalPM,
                                                                  Pageable pageable);

    // 6. 상태 + 제안 PM
    @Query("SELECT mp FROM MemberProject mp " +
            "JOIN FETCH mp.project p " +
            "WHERE mp.member.memberId = :memberId " +
            "AND p.status = :status " +
            "AND LOWER(p.proposalPM) LIKE LOWER(CONCAT('%', :proposalPM, '%')) " +
            "AND p.isDeleted = false")
    Page<MemberProject> findByMemberIdAndStatusAndProposalPM(@Param("memberId") Long memberId,
                                                             @Param("status") ProjectStatus status,
                                                             @Param("proposalPM") String proposalPM,
                                                             Pageable pageable);

    // 7. 모든 필터 조건
    @Query("SELECT mp FROM MemberProject mp " +
            "JOIN FETCH mp.project p " +
            "WHERE mp.member.memberId = :memberId " +
            "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :projectName, '%')) " +
            "AND p.status = :status " +
            "AND LOWER(p.proposalPM) LIKE LOWER(CONCAT('%', :proposalPM, '%')) " +
            "AND p.isDeleted = false")
    Page<MemberProject> findByMemberIdAndAllFilters(@Param("memberId") Long memberId,
                                                    @Param("projectName") String projectName,
                                                    @Param("status") ProjectStatus status,
                                                    @Param("proposalPM") String proposalPM,
                                                    Pageable pageable);

    // 특정 프로젝트와 멤버 아이디로 조회 (삭제 여부 필터 필요하면 추가 가능)
    @Query("SELECT mp FROM MemberProject mp " +
            "JOIN FETCH mp.project p " +
            "WHERE p.id = :projectId " +
            "AND mp.member.id = :id " +
            "AND p.isDeleted = false")
    Optional<MemberProject> findByProjectIdAndId(@Param("projectId") long projectId, @Param("id") String memberId);

    // 어드민 여부 조회
    Boolean existsByMemberAndIsAdminTrue(Member member);

	List<MemberProject> project(Project project);

    MemberProject findByProject_ProjectIdAndMember_MemberId(Long projectId, Long memberId);
}
