package com.skala.decase.domain.source.domain;

import com.skala.decase.domain.document.domain.Document;
import com.skala.decase.domain.requirement.domain.Requirement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

@Entity
@Table(name = "TD_SOURCE")
@Getter
@Setter
@Audited
@NoArgsConstructor
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "req_pk", nullable = false)
    private Requirement requirement;

    private String reqIdCode;

    private int revisionCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Document document;

    private int pageNum;  //페이지 번호

    @Column(name = "rel_sentence", length = 1000, nullable = false)
    private String relSentence;  //관련 문장

    public void createSource(Requirement requirement, Document document, int pageNum, String relSentence, int revisionCount) {
        this.requirement = requirement;
        this.reqIdCode = requirement.getReqIdCode();
        this.document = document;
        this.pageNum = pageNum;
        this.relSentence = relSentence;
        this.revisionCount = revisionCount;
    }

}
