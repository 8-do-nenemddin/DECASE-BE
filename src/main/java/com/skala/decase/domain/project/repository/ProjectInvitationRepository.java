package com.skala.decase.domain.project.repository;

import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.domain.ProjectInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, Long> {
    ProjectInvitation findByToken(String token);

    List<ProjectInvitation> findAllByProject(Project project);

    Optional<ProjectInvitation> findFirstByProjectAndEmailOrderByExpiryDateDesc(Project project, String email);
}
