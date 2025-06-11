package com.skala.decase.domain.requirement.controller.dto.request;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.requirement.domain.Difficulty;
import com.skala.decase.domain.requirement.domain.Priority;
import com.skala.decase.domain.requirement.domain.Requirement;
import com.skala.decase.domain.requirement.domain.RequirementType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateRequirementDto {
	private Long memberId;
	private Long reqPk;
	private RequirementType type;
	private String level1;
	private String level2;
	private String level3;
	private Priority priority;
	private Difficulty difficulty;
	private String name;
	private String description;
	private boolean isDeleted;
	private String modReason;

	public Requirement toEntity(Project project, String reqIdCode, int revisionCount, Member member) {
		Requirement requirement = new Requirement();
		requirement.setProject(project);
		requirement.setReqIdCode(reqIdCode);
		requirement.setRevisionCount(revisionCount);
		requirement.setCreatedBy(member);
		requirement.setModReason(this.getModReason());
		requirement.setType(this.getType());
		requirement.setLevel1(this.getLevel1());
		requirement.setLevel2(this.getLevel2());
		requirement.setLevel3(this.getLevel3());
		requirement.setName(this.getName());
		requirement.setDescription(this.getDescription());
		requirement.setPriority(this.getPriority());
		requirement.setDifficulty(this.getDifficulty());
		requirement.setDeleted(isDeleted);
		requirement.setCreatedDate(LocalDateTime.now());
		requirement.setDeletedRevision(0);
		return requirement;
	}
}