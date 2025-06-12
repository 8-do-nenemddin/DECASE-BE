package com.skala.decase.domain.project.domain;

import com.skala.decase.domain.document.domain.Document;
import com.skala.decase.domain.requirement.domain.Requirement;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.scheduling.annotation.Scheduled;

@Entity
@Audited
@Table(name = "TM_PROJECTS")
@Data
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id", nullable = false)
    private long projectId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private long scale;  //프로젝트 규모

    @Column(nullable = false)
    private Date startDate;  //프로젝트 시작일

    @Column(nullable = false)
    private Date endDate;  //프로젝트 종료일

    @Column(name = "description", length = 1000, nullable = false)
    private String description;  //설명

    @Column(name = "proposal_pm", length = 100)
    private String proposalPM;  //제안 PM

    @Column(nullable = false, columnDefinition = "INT DEFAULT 1")
    private int revisionCount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    @NotAudited
    @Column(nullable = false)
    private LocalDateTime createdDate;

    @NotAudited
    @Column(nullable = false)
    private LocalDateTime modifiedDate;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    private List<MemberProject> membersProjects;

    @NotAudited
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents;

    @NotAudited
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Requirement> requirements;

    // 프로젝트 생성자
    public Project(String name, Long scale, Date startDate, Date endDate,
                   String description, String proposalPM, LocalDateTime createdDate, LocalDateTime modifiedDate) {
        this.name = name;
        this.scale = scale;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.proposalPM = proposalPM;
        this.revisionCount = 1;
        this.status = ProjectStatus.NOT_STARTED;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.membersProjects = new ArrayList<>();
        this.documents = new ArrayList<>();
        this.requirements = new ArrayList<>();
    }
}
