package com.skala.decase.domain.requirement.controller.dto.request;

import com.skala.decase.domain.requirement.domain.Requirement;
import lombok.Data;

@Data
public class RequirementDto {
	private long reqPk;
	private String reqIdCode;
	private int revisionCount;
	private String type;
	private String status;
	private String level1;
	private String level2;
	private String level3;
	private String priority;
	private String difficulty;
	private String name;
	private String description;
	private String createdDate;
	private int deletedRevision;
	private String modReason;

	public RequirementDto fromEntity(Requirement r) {
		RequirementDto dto = new RequirementDto();
		dto.setReqPk(r.getReqPk());
		dto.setReqIdCode(r.getReqIdCode());
		dto.setRevisionCount(r.getRevisionCount());
		dto.setType(r.getType().name());
		dto.setStatus(r.getType().name());
		dto.setLevel1(r.getLevel1());
		dto.setLevel2(r.getLevel2());
		dto.setLevel3(r.getLevel3());
		dto.setPriority(r.getPriority() != null ? r.getPriority().name() : null);
		dto.setDifficulty(r.getDifficulty() != null ? r.getDifficulty().name() : null);
		dto.setName(r.getName());
		dto.setDescription(r.getDescription());
		dto.setCreatedDate(r.getCreatedDate().toString());
		dto.setDeletedRevision(r.getDeletedRevision());
		dto.setModReason(r.getModReason());
		return dto;
	}
}
