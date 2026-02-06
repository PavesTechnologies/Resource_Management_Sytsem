package com.repo.project_repo;

import com.entity.project_entities.ProjectEscalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectEscalationRepo extends JpaRepository<ProjectEscalation, UUID> {
    List<ProjectEscalation> findByProject_PmsProjectId(Long projectId);
}
