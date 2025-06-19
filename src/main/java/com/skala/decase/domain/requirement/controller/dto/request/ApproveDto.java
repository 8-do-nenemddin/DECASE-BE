package com.skala.decase.domain.requirement.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveDto {
	private Long pendingPk;  // 승인/반려할 PendingRequirement PK
	private int status;  // true: 승인, false: 반려
}