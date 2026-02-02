package com.cdc.listener;

import com.entity.Project;
import com.repo.ProjectRepository;
import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class ProjectCdcHandler {

    private final ProjectRepository projectRepository;

    public ProjectCdcHandler(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public void handleEvent(RecordChangeEvent<SourceRecord> event) {

        SourceRecord record = event.record();
        Struct value = (Struct) record.value();
        if (value == null) return;

        String op = value.getString("op"); // c, u, d, r

        // Ignore snapshot read events
        if ("r".equals(op)) return;

        // INSERT or UPDATE
        if ("c".equals(op) || "u".equals(op)) {

            Struct after = value.getStruct("after");
            if (after == null) return;

            // PMS: projects.id
            Long projectId = ((Number) after.get("id")).longValue();

            Optional<Project> existing = projectRepository.findById(projectId);

            if (existing.isEmpty()) {

                LocalDateTime now = LocalDateTime.now();

                Project p = Project.builder()
                        // 🔑 from PMS
                        .projectId(projectId)

                        // 🧱 REQUIRED RMS FIELDS (DEFAULT STRINGS)
                        .projectName("PENDING_FROM_PMS")
                        .clientId(null) // if nullable in DB, else set -1
                        .projectStatus("DRAFT")
                        .lifecycleStage("INITIATION")

                        .deliveryModel("OFFSHORE")
                        .primaryLocation("UNKNOWN")

                        .projectManagerId(null)
                        .resourceManagerId(null)
                        .deliveryLeadId(null)

                        .riskLevel("LOW")
                        .priorityLevel("LOW")

                        // 🔄 CDC metadata
                        .sourceSystem("PMS")
                        .externalRefId("PMS_" + projectId)

                        // 🕒 audit
                        .createdAt(now)
                        .updatedAt(now)
                        .lastSyncedAt(now)

                        .build();

                projectRepository.save(p);

            } else {
                Project p = existing.get();
                p.setLastSyncedAt(LocalDateTime.now());
                projectRepository.save(p);
            }
            return;
        }

        // DELETE
        if ("d".equals(op)) {

            Struct before = value.getStruct("before");
            if (before == null) return;

            Long projectId = ((Number) before.get("id")).longValue();
            projectRepository.archiveByProjectId(projectId);
        }
    }
}
