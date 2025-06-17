package com.skala.decase.domain.requirement.domain;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.domain.Project;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

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

	@Column(nullable = false, columnDefinition = "INT DEFAULT 1")
	private int revisionCount;

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

	@Column(nullable = false)
	private LocalDateTime createdDate;

	@LastModifiedDate
	private LocalDateTime modifiedDate;

	private String modReason;   //수정 사유

	private Boolean status;
}
