package com.skala.decase.domain.requirement.domain;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.requirement.controller.dto.request.UpdateRequirementDto;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Data
@Entity
@Table(name = "TD_PENDING_REQUIREMENTS")
@EntityListeners(AuditingEntityListener.class)
public class PendingRequirement {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "pending_pk", nullable = false)
	private Long pendingPk;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(name = "req_id_code", length = 100, nullable = false)
	private String reqIdCode;

	@Column(name = "name", length = 100, nullable = false)
	private String name;  // 요구사항 명

	@Column(name = "description", length = 5000)
	private String description;  //요구사항 설명

	@Enumerated(EnumType.STRING)
	private RequirementType type;

	@Column(name = "level_1", length = 100)
	private String level1;

	@Column(name = "level_2", length = 100)
	private String level2;

	@Column(name = "level_3", length = 100)
	private String level3;

	@Enumerated(EnumType.STRING)
	private Priority priority;

	@Enumerated(EnumType.STRING)
	private Difficulty difficulty;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member createdBy;

	@LastModifiedDate
	private LocalDateTime modifiedDate;

	private String modReason;   //수정 사유

	private Boolean status;

	public void createPendingRequirement(UpdateRequirementDto req, String reqIdCode, Project project, Member createdBy) {
		this.project = project;
		this.reqIdCode = reqIdCode;
		this.name = req.getName();
		this.description = req.getDescription();
		this.type = req.getType();
		this.level1 = req.getLevel1();
		this.level2 = req.getLevel2();
		this.level3 = req.getLevel3();
		this.priority = req.getPriority();
		this.difficulty = req.getDifficulty();
		this.createdBy = createdBy;
		this.modifiedDate = LocalDateTime.now();
		this.modReason = req.getModReason();
		this.status = false;
	}
}
