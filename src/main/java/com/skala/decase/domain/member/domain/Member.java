package com.skala.decase.domain.member.domain;

import com.skala.decase.domain.company.domain.Company;
import com.skala.decase.domain.department.domain.Department;
import com.skala.decase.domain.project.domain.MemberProject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Audited
@Table(name = "TN_MEMBERS")
@Getter
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false)
    private long memberId;

    @Column(name = "id", length = 30, nullable = false)
    private String id;

    @Column(name = "password", length = 30, nullable = false)
    private String password;

    @Column(name = "name", length = 10, nullable = false)
    private String name;

    @Column(name = "email", length = 50, nullable = false)
    private String email;

    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<MemberProject> membersProjects;

    // 회원가입 용 생성자
    public Member(String id, String password, String name, String email, Company company, Department department) {
        this.id = id;
        this.password = password;
        this.name = name;
        this.email = email;
        this.company = company;
        this.department = department;
    }
}
