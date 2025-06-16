package com.skala.decase.domain.requirement.controller;

import com.skala.decase.domain.requirement.controller.dto.response.PendingRequirementDto;
import com.skala.decase.domain.requirement.controller.dto.response.RequirementWithSourceResponse;
import com.skala.decase.domain.requirement.service.PendingRequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Pending Requirement API", description = "요청된 요구사항 관리를 위한 api 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/")
public class PendingRequriementController {

	private final PendingRequirementService pendingRequirementService;

	@Operation(summary = "[Admin] 수정 요청 요구사항 목록", description = "수정 요청된 요구사항을 조회합니다.")
	@GetMapping("/{projectId}/requirements/pending")
	public List<PendingRequirementDto> getPendingRequirements(
			@PathVariable Long projectId) {
		return pendingRequirementService.getPendingRequirementsList(projectId);
	}
}
