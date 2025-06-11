package com.skala.decase.domain.mockup.repository;

import com.skala.decase.domain.mockup.domain.Mockup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockupRepository extends JpaRepository<Mockup, Long> {
	List<Mockup> findAllByProject_ProjectId(Long projectId);
	List<Mockup> findAllByProject_ProjectIdAndRevisionCount(Long projectId, Integer revisionCount);
	Optional<Mockup> findByProject_ProjectIdAndRevisionCountAndName(Long ProjectId, Integer revisionCount, String fileName);
	Boolean existsByProject_ProjectIdAndRevisionCount(Long projectId, Integer revisionCount);
}
