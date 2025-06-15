package com.skala.decase.domain.requirement.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RequirementRevisionDto {
	private String label;
	private int revision;
	private String date;
}