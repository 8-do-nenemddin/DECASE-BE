package com.skala.decase.domain.project.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.skala.decase.domain.project.domain.Project;
import lombok.Data;

import java.util.Date;

@Data
public class EditProjectResponseDto {
	private Long projectId;
	private String name;
	private Long scale;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date startDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date endDate;

	private String description;
	private String proposalPM;

	public static EditProjectResponseDto fromEntity(Project project) {
		EditProjectResponseDto dto = new EditProjectResponseDto();
		dto.setProjectId(project.getProjectId());
		dto.setName(project.getName());
		dto.setScale(project.getScale());
		dto.setStartDate(project.getStartDate());
		dto.setEndDate(project.getEndDate());
		dto.setDescription(project.getDescription());
		dto.setProposalPM(project.getProposalPM());
		return dto;
	}
}