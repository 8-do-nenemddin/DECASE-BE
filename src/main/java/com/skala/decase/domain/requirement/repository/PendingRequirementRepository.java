package com.skala.decase.domain.requirement.repository;

import com.skala.decase.domain.requirement.controller.dto.response.RequirementDto;
import com.skala.decase.domain.requirement.domain.PendingRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PendingRequirementRepository extends JpaRepository<PendingRequirement, Long> {
	List<PendingRequirement> findAllByProject_ProjectIdAndStatusFalse(Long projectId);

}
