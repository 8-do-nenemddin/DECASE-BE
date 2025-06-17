package com.skala.decase.domain.requirement.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRequirementDto {
	private RequirementDto original; // 기존 요구사항
	private RequirementDto proposed; // 수정 요청된 요구사항
}
