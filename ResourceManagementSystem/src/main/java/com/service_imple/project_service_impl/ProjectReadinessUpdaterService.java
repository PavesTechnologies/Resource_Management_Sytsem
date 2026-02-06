package com.service_imple.project_service_impl;

import com.dto.project_dto.ReadinessStatusDTO;
import com.entity.project_entities.Project;
import com.repo.project_repo.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProjectReadinessUpdaterService {

    private final ProjectRepository projectRepository;
    private final ProjectReadinessCalculationService calculationService;

    @Transactional
    public void updateReadiness(Project project) {

        ReadinessStatusDTO readiness =
                calculationService.calculate(project);

        project.setStaffingReadinessStatus(readiness.getStatus());
        project.setStaffingReadinessReason(readiness.getReason());
        project.setStaffingReadinessUpdatedAt(LocalDateTime.now());

        projectRepository.save(project);
    }
}

