package com.skala.decase.domain.document.domain;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.source.domain.Source;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "TM_DOCUMENTS")
@NoArgsConstructor
public class Document {

    /**
     * doc_id 규칙
     * - rfp 파일: RFP-{숫자}
     * - 회의록 음성 : MOMV-{숫자}
     * - 회의록 문서 : MOMD-{숫자}
     * - 추가 파일 : EXTRA-{숫자}
     * - 요구사항 정의서 : REQ-{숫자}
     * - 조견표 : QFS-{숫자}
     * - 매트릭스 : MATRIX-{숫자}
     * - As-is : ASIS-{숫자}
     */
    @Id
    @Column(name = "doc_id", nullable = false)
    private String docId;

    @Column(name = "path", length = 1000, nullable = false)
    private String path;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "doc_description", length = 5000)
    private String docDescription;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private boolean isMemberUpload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", updatable = false)
    private Member createdBy;

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Source> sources;  // 출처

    public Document(String docId, String name, String path, boolean isMemberUpload, Project project, Member createdBy) {
        this.docId = docId;
        this.name = name;
        this.path = path;
        this.createdDate = LocalDateTime.now();
        this.isMemberUpload = isMemberUpload;
        this.createdBy = createdBy;
        this.project = project;
    }
}
