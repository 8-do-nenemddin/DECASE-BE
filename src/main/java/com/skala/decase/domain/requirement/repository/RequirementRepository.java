package com.skala.decase.domain.requirement.repository;

import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.requirement.domain.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RequirementRepository extends JpaRepository<Requirement, Long> {
	List<Requirement> findByProject_AndRevisionCountAndIsDeletedFalse(Project project, int revisionCount);
	List<Requirement> findByProject_AndIsDeletedFalse(Project project);

	@Query("SELECT MAX(r.revisionCount) FROM Requirement r WHERE r.project = :project")
	Integer getMaxRevisionCount(Project project);

	Optional<Requirement> findFirstByProjectAndRevisionCount(Project project, int revisionCount);
}
