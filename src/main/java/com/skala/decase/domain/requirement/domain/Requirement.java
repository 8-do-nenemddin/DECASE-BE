package com.skala.decase.domain.requirement.domain;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.domain.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Audited
@Table(name = "TD_REQUIREMENTS")
@Getter
@NoArgsConstructor
public class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "req_pk", nullable = false)
    private long reqPk;

    @Audited
    @Column(name = "req_id_code", length = 100, nullable = false)
    private String reqIdCode;

    @Audited
    @Column(nullable = false, columnDefinition = "INT DEFAULT 1")
    private int revisionCount;

    @Audited
    @Enumerated(EnumType.STRING)
    private RequirementType type;

    @Audited
    @Column(name = "level_1", length = 100)
    private String level1;

    @Audited
    @Column(name = "level_2", length = 100)
    private String level2;

    @Audited
    @Column(name = "level_3", length = 100)
    private String level3;

    @Audited
    @Column(name = "name", length = 100, nullable = false)
    private String name;  // 요구사항 명

    @Audited
    @Column(name = "description", length = 5000)
    private String description;  //요구사항 설명

    @Audited
    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Audited
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @NotAudited
    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Audited
    @Column(nullable = false)
    private LocalDateTime modifiedDate;

    @Audited
    @Column(columnDefinition = "boolean DEFAULT false")
    private boolean isDeleted;  //요구사항 삭제 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Member createdBy;

    @Audited
    private String modReason;   //수정 사유

    // 양방향 관계
    @OneToMany(mappedBy = "requirement", fetch = FetchType.LAZY)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private List<RequirementDocument> requirementDocuments;  //출처
}
