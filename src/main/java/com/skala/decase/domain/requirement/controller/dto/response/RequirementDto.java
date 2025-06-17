package com.skala.decase.domain.requirement.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementDto {
	private Long id;
	private String idCode;
	private String type;
	private String name;
	private String description;
	private String category1;
	private String category2;
	private String category3;
	private String priority;
	private String difficulty;
	private String source;
	private String sourcePage;
	private String modifiedDate;
	private String modifier;
	private String reason;
}
