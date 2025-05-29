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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TD_REQUIREMNETS")
@Getter
@NoArgsConstructor
public class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "req_pk")
    private long reqPk;

    @Column(name = "req_id_code", length = 100)
    private String reqIdCode;

    private int revisionCount;

    @Enumerated(EnumType.STRING)
    private RequirementType type;

    @Column(name = "level_1", length = 100)
    private String level1;

    @Column(name = "level_2", length = 100)
    private String level2;

    @Column(name = "level_3", length = 100)
    private String level3;

    @Column(name = "name", length = 100)
    private String name;  // 요구사항 명

    @Column(name = "description", length = 5000)
    private String description;  //요구사항 설명

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    private boolean isDeleted;  //요구사항 삭제 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member createdBy;

    private String modReason;   //수정 사유

    // 양방향 관계
    @OneToMany(mappedBy = "requirement", fetch = FetchType.LAZY)
    private List<RequirementDocument> requirementDocuments;  //출처
}
