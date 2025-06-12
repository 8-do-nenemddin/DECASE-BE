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
	private Long memberId; // createdBy  변경
	private Long reqPk; // 변경 X
	private RequirementType type; // 변경
	private String level1; // 변경
	private String level2; // 변경
	private String level3; // 변경
	private Priority priority; // 변경
	private Difficulty difficulty; // 변경
	private String name; // 변경
	private String description; // 변경
 	private boolean isDeleted;
	private String modReason; // 변경

	public Requirement toEntity(Project project, String reqIdCode, int revisionCount, Member member) {
		Requirement requirement = new Requirement();
		requirement.setProject(project);
		requirement.setProjectIdAud(project.getProjectId());
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