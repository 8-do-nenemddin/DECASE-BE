package com.skala.decase.domain.requirement.repository;

import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.domain.RequirementDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequirementDocumentRepository extends JpaRepository<RequirementDocument, Long> {
	RequirementDocument findByRequirement(Requirement requirement);

	List<RequirementDocument> findAllByRequirement(Requirement requirement);
}
