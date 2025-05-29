package com.skala.decase.domain.document.repository;

import com.skala.decase.domain.document.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, String> {
	@Query("SELECT d.docId FROM Document d WHERE d.docId LIKE CONCAT(:prefix, '-%') ORDER BY d.docId DESC LIMIT 1")
	Optional<String> findLatestDocIdByPrefix(@Param("prefix") String prefix);
}