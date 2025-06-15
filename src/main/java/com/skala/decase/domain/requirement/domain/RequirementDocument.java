package com.skala.decase.domain.requirement.domain;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.skala.decase.domain.document.domain.Document;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TD_SOURCE")
@Getter
@Setter
@NoArgsConstructor
@Audited
public class RequirementDocument {

    @Id
    @Column(name = "source_id")
    private long sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "req_pk")
    private Requirement requirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", insertable = false, updatable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Document document;

    private int pageNum;  //페이지 번호

    private String relSentence;  //관련 문장

}
