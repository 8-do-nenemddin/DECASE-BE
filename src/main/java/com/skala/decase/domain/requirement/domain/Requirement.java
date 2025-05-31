package com.skala.decase.domain.requirement.domain;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.domain.Project;
import jakarta.persistence.*;

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
    @Column(name = "req_pk", nullable = false)
    private long reqPk;

    @Column(name = "req_id_code", length = 100, nullable = false)
    private String reqIdCode;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 1")
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

    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime modifiedDate;

    @Column(columnDefinition = "boolean DEFAULT false")
    private boolean isDeleted;  //요구사항 삭제 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member createdBy;

    private String modReason;   //수정 사유

    // 양방향 관계
    @OneToMany(mappedBy = "requirement", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequirementDocument> requirementDocuments;  //출처
}
