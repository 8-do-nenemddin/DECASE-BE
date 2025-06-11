package com.skala.decase.domain.requirement.domain;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.source.domain.Source;
import jakarta.persistence.CascadeType;
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
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import lombok.NoArgsConstructor;

@Entity
@Audited
@Table(name = "TD_REQUIREMENTS")
@Data
@NoArgsConstructor
public class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "req_pk", nullable = false)
    private long reqPk;

    @Column(name = "req_id_code", length = 100, nullable = false)
    private String reqIdCode;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int revisionCount;

    @Enumerated(EnumType.STRING)
    private RequirementType type;

    @Column(name = "level_1", length = 100)
    private String level1;

    @Column(name = "level_2", length = 100)
    private String level2;

    @Column(name = "level_3", length = 100)
    private String level3;

    @Column(name = "name", length = 100, nullable = false)
    private String name;  // 요구사항 명

    @Column(name = "description", length = 5000)
    private String description;  //요구사항 설명

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @NotAudited
    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Column(columnDefinition = "boolean DEFAULT false")
    private boolean isDeleted;  //요구사항 삭제 여부

    @Column(nullable = false)
    private int deletedRevision;  // 요구사항이 삭제된 버전 정보

    // Envers 감사 전용
    @Column(name = "project_id_aud")
    private Long projectIdAud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @NotAudited
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member createdBy;

    private String modReason;   //수정 사유

    // 양방향 관계
    @OneToMany(mappedBy = "requirement", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private List<Source> sources;  //출처

    /**
     * 요구사항 정의서 soft delete
     */
    public void softDelete(int deletedRevision) {
        this.isDeleted = true;
        this.deletedRevision = deletedRevision;
    }

    /**
     * 요구사항 정의서 초기 생성시 생성되는 데이터
     */
    public void createInitialRequirement(String reqIdCode, RequirementType type, String level1, String level2,
                                         String level3, String name, String description, Priority priority,
                                         Difficulty difficulty,
                                         LocalDateTime createdDate, Project project, Member createdBy) {
        this.reqIdCode = reqIdCode;
        this.revisionCount = 1;
        this.type = type;
        this.level1 = level1;
        this.level2 = level2;
        this.level3 = level3;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.difficulty = difficulty;
        this.createdDate = createdDate;
        this.deletedRevision = 0;  //초기 요구사항은 삭제 x
        this.isDeleted = false;
        this.project = project;
        this.projectIdAud = project.getProjectId();
        this.createdBy = createdBy;
        this.modReason = ""; //초기 요구사항 정의서의 수정 이유는 비워둠.
        this.sources = new ArrayList<>();
    }


    /**
     * 요구사항 정의서 수정시 추가되는 데이터
     */
    public void createUpdateRequirement(String reqIdCode, int revisionCount, String modReason, RequirementType type,
                                        String level1, String level2,
                                        String level3, String name, String description, Priority priority,
                                        Difficulty difficulty,
                                        LocalDateTime createdDate, Project project, Member createdBy) {
        this.reqIdCode = reqIdCode;
        this.revisionCount = revisionCount;
        this.type = type;
        this.level1 = level1;
        this.level2 = level2;
        this.level3 = level3;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.difficulty = difficulty;
        this.createdDate = createdDate;
        this.isDeleted = false;
        this.project = project;
        this.projectIdAud = project.getProjectId();
        this.createdBy = createdBy;
        this.modReason = modReason; //요구사항 추가 이유
    }
}
