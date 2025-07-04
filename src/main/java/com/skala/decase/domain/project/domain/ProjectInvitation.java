package com.skala.decase.domain.project.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Audited
@Table(name = "TM_PROJECTS_INVITATION")
@Getter
@NoArgsConstructor
public class ProjectInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String token;
    private boolean accepted = false;
    private LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    private Permission permission;

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @Builder
    public ProjectInvitation(String email, boolean accepted, Permission permission, Project project) {
        this.email = email;
        this.token = UUID.randomUUID().toString();
        this.accepted = accepted;
        this.expiryDate = LocalDateTime.now().plusDays(7); //일주일 이내에 초대에 응답 해야 합니다.
        this.permission = permission;
        this.project = project;
    }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDateTime.now());
    }

    public void setAcceptedTrue() {
        this.accepted = true;
    }
}
