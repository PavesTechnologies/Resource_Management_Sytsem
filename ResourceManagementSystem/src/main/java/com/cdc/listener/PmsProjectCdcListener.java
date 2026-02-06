package com.cdc.listener;

import com.cdc.event.PmsProjectAfter;
import com.cdc.event.PmsProjectCdcEvent;
import com.entity.project_entities.Project;
import com.repo.project_repo.ProjectRepository;
import com.service_imple.project_service_impl.ProjectReadinessUpdaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PmsProjectCdcListener {

    private final ProjectRepository projectRepository;
    private final ProjectReadinessUpdaterService readinessUpdater;

    @KafkaListener(
            topics = "pms.project",
            groupId = "rms-project-sync"
    )
    @Transactional
    public void onProjectChange(PmsProjectCdcEvent event) {

        if (event.getPayload().getAfter() == null) {
            return; // delete or tombstone
        }

        PmsProjectAfter after = event.getPayload().getAfter();

        Project project = projectRepository
                .findById(after.getPmsProjectId())
                .orElse(new Project());

        // 🔹 Map PMS fields
        project.setPmsProjectId(after.getPmsProjectId());
        project.setName(after.getName());
        project.setProjectStatus(after.getProjectStatus());
        project.setStartDate(after.getStartDate());
        project.setEndDate(after.getEndDate());
        project.setDataStatus(after.getDataStatus());
        project.setLastSyncedAt(LocalDateTime.now());

        // 🔹 Save PMS data
        projectRepository.save(project);

        // 🔥 CRITICAL: update readiness AFTER save
        readinessUpdater.updateReadiness(project);
    }
}

