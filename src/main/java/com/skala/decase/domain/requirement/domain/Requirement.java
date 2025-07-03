package com.skala.decase.domain.requirement.domain;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.requirement.controller.dto.request.SrsDeleteRequestDetail;
import com.skala.decase.domain.requirement.controller.dto.request.SrsUpdateRequestDetail;
import com.skala.decase.domain.requirement.controller.dto.request.UpdateRequirementDto;
import com.skala.decase.domain.source.domain.Source;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Audited
@Table(name = "TD_REQUIREMENTS")
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "req_pk", nullable = false)
    private long reqPk;

    @Column(name = "req_id_code", length = 100, nullable = false)
    private String reqIdCode;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 1")
    private int revisionCount;

    @Enumerated(EnumType.STRING)
    private RequirementType type;

    @Enumerated(EnumType.STRING)
    private Reception status = Reception.ACCEPTED;

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

    @CreatedDate
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id_mod", nullable = true)
    private Member modifiedBy;

    @LastModifiedDate
    private LocalDateTime modifiedDate;

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
    @Builder
    public Requirement(String reqIdCode, RequirementType type, String level1, String level2,
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
        this.modifiedBy = createdBy;
        this.modReason = "RFP로부터 생성"; //초기 요구사항 정의서의 수정 이유는 비워둠.
        this.sources = new ArrayList<>();
    }


    /**
     * 요구사항 정의서 수정시 추가되는 데이터
     */
    public void createUpdateRequirement(String reqIdCode, int revisionCount, String modReason, RequirementType type,
                                        String level1, String level2,
                                        String level3, String name, String description, Priority priority,
                                        Difficulty difficulty,
                                        LocalDateTime createdDate, Project project, Member modifiedBy) {
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
        this.createdBy = modifiedBy;
        this.modifiedBy = modifiedBy;
        this.modReason = modReason; //요구사항 추가 이유
    }

    public void update(UpdateRequirementDto requirementDto, Member modifiedBy) {
        this.type = requirementDto.getType() == null ? this.type : requirementDto.getType();
        this.level1 = requirementDto.getLevel1() == null ? this.level1 : requirementDto.getLevel1();
        this.level2 = requirementDto.getLevel2() == null ? this.level2 : requirementDto.getLevel2();
        this.level3 = requirementDto.getLevel3() == null ? this.level3 : requirementDto.getLevel3();
        this.priority = requirementDto.getPriority() == null ? this.priority : requirementDto.getPriority();
        this.difficulty = requirementDto.getDifficulty() == null ? this.difficulty : requirementDto.getDifficulty();
        this.status = requirementDto.getStatus() == null ? this.status : Reception.fromKorean(requirementDto.getStatus());
        this.name = requirementDto.getName() == null ? this.getName() : requirementDto.getName();
        this.description = requirementDto.getDescription() == null ? this.description : requirementDto.getDescription();
        this.modReason = requirementDto.getModReason();
        this.modifiedBy = modifiedBy;
    }

    /**
     * 요구사항 정의서 내용 수정
     */
    public void updateSRS(SrsUpdateRequestDetail response, Member modifiedBy) {
        this.revisionCount += 1;
        if (response.modified_reason() != null) {
            this.modReason = response.modified_reason();
        }
        if (response.type() != null) {
            this.type = RequirementType.fromKorean(response.type());
        }
        if (response.level1() != null) {
            this.level1 = response.level1();
        }
        if (response.level2() != null) {
            this.level2 = response.level2();
        }
        if (response.level3() != null) {
            this.level3 = response.level3();
        }
        if (response.requirement_name() != null) {
            this.name = response.requirement_name();
        }
        if (response.importance() != null) {
            this.priority = Priority.fromEnglish(response.importance());
        }
        if (response.difficulty() != null) {
            this.difficulty = Difficulty.fromEnglish(response.difficulty());
        }
        if (response.description() != null) {
            this.description = response.description();
        }
        this.modifiedBy = modifiedBy;
    }

    /**
     * 요구사항 정의서 내용 삭제
     */
    public void deleteSRS(SrsDeleteRequestDetail response, Member modifiedBy) {
        this.revisionCount += 1;
        this.isDeleted = true;
        this.deletedRevision = this.revisionCount;
        if (response.modified_reason() != null) {
            this.modReason = response.modified_reason();
        }
        this.modifiedBy = modifiedBy;
    }

    public void updateFromPending(RequirementType newType, String newLevel1, String newLevel2, String newLevel3, String newName,
                                  String newDescription, Priority newPriority, Difficulty newDifficulty, String modReason,
                                  Member modifiedBy) {
        this.type = newType == null ? this.type : newType;
        this.level1 = newLevel1 == null ? this.level1 : newLevel1;
        this.level2 = newLevel2 == null ? this.level2 : newLevel2;
        this.level3 = newLevel3 == null ? this.level3 : newLevel3;
        this.name = newName == null ? this.name : newName;
        this.description = newDescription == null ? this.description : newDescription;
        this.priority = newPriority == null ? this.priority : newPriority;
        this.difficulty = newDifficulty == null ? this.difficulty : newDifficulty;
        this.modReason = modReason;
        this.modifiedBy = modifiedBy;
    }
}
