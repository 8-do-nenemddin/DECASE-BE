package com.skala.decase.domain.document.repository;

import com.skala.decase.domain.document.domain.Document;
import com.skala.decase.domain.project.domain.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, String> {
    @Query("SELECT d.docId FROM Document d WHERE d.docId LIKE CONCAT(:prefix, '-%') ORDER BY d.docId DESC LIMIT 1")
    Optional<String> findLatestDocIdByPrefix(@Param("prefix") String prefix);

    @Query("SELECT d FROM Document d WHERE d.docId =:docId ")
    Optional<Document> findByDocId(@Param("docId") String docId);

    List<Document> findAllByProjectAndIsMemberUploadTrue(Project project);

    /**
     * as-is 문서 리스트 조회
     */
    @Query("SELECT d FROM Document d WHERE d.project=:project AND d.docId LIKE 'VASIS-%'")
    List<Document> findByDocIdStartingWithASIS(@Param("project") Project project);

    /**
     * 파일명으로 문서 조회 (확장자 제외)
     */
    @Query("SELECT d FROM Document d WHERE d.project=:project AND d.name LIKE :fileName")
    List<Document> findByProjectAndName(@Param("project") Project project, @Param("fileName") String fileName);
}