package com.cdc.listener;

import com.cdc.mapping.ColumnMapping;
import com.cdc.mapping.ProjectCdcMappingRegistry;
import com.cdc.util.CdcValueConverter;
import com.cdc.util.DebeziumChangeDetector;
import com.cdc.util.ReflectionUtil;
import com.entity.project_entities.Project;
import com.entity_enums.project_enums.ProjectStatus;
import com.repo.client_repo.ClientRepo;
import com.repo.project_repo.ProjectRepository;
import com.service_imple.project_service_impl.ProjectReadinessUpdaterService;
import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Component
public class ProjectCdcHandler {

    private final ProjectRepository projectRepository;
    private final ProjectReadinessUpdaterService readinessUpdater;
    private final ClientRepo clientRepo;

    public ProjectCdcHandler(ProjectRepository projectRepository, ProjectReadinessUpdaterService readinessUpdater, ClientRepo clientRepo) {
        this.projectRepository = projectRepository;
        this.readinessUpdater = readinessUpdater;
        this.clientRepo = clientRepo;
    }

    // ... existing imports and class setup

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEvent(RecordChangeEvent<SourceRecord> event) {
        Struct value = (Struct) event.record().value();
        if (value == null) return;

        String op = value.getString("op");
        if ("r".equals(op)) return;

        Struct before = value.getStruct("before");
        Struct after  = value.getStruct("after");

        // DELETE (soft delete) - logic remains the same
        if ("d".equals(op)) {
            handleDelete(before);
            return;
        }

        // INSERT or UPDATE
        if (after == null) return;

        Long pmsProjectId = ((Number) after.get("id")).longValue();
        // Fix: Extract name from the Debezium 'after' struct
        String projectName = after.getString("name");
        
        // Extract clientId from Debezium event
        UUID clientId = null;
        Object clientIdValue = after.get("client_id");
        if (clientIdValue != null) {
            try {
                clientId = UUID.fromString(clientIdValue.toString());
                // Validate client exists
                if (!clientRepo.existsById(clientId)) {
                    clientId = null; // Don't set invalid clientId
                }
            } catch (Exception e) {
                clientId = null; // Invalid UUID format
            }
        }

        // ✅ STEP 1: ATOMIC UPSERT (Thread-safe at DB level)
        projectRepository.upsertSkeleton(
                pmsProjectId,
                projectName != null ? projectName : "New Project",
                LocalDateTime.now(),
                clientId
        );

        // ✅ STEP 2: PESSIMISTIC LOCK (Multi-instance safe)
        // This blocks other instances from editing this specific row until this method finishes
        Project rmsProject = projectRepository.findByIdWithLock(pmsProjectId)
                .orElse(null);
        
        if (rmsProject == null) {
            // Log the issue and return gracefully - don't mark transaction for rollback
            System.err.println("Entity not found after upsert for PMS project ID: " + pmsProjectId);
            return;
        }

        // STEP 3: Detect changed columns
        Set<String> changedColumns = DebeziumChangeDetector.detectChangedColumns(before, after);

        // STEP 4: Apply mapped changes
        for (String pmsColumn : changedColumns) {
            ColumnMapping mapping = ProjectCdcMappingRegistry.PMS_TO_RMS.get(pmsColumn);
            if (mapping == null) continue;

            // Skip client_id as it's already handled in upsertSkeleton
            if ("client_id".equals(pmsColumn)) {
                continue;
            }

            Object rawValue = after.schema().field(pmsColumn) != null ? after.get(pmsColumn) : null;
            Object converted = CdcValueConverter.convert(rawValue, mapping.getFieldType(), mapping.getEnumClass());

            try {
                ReflectionUtil.setField(rmsProject, mapping.getRmsField(), converted);
            } catch (Exception e) {
                // Log the error but continue processing other fields
                System.err.println("Failed to set field " + mapping.getRmsField() + " for project " + pmsProjectId + ": " + e.getMessage());
            }
        }

        // STEP 5: Audit and Persist
        rmsProject.setLastSyncedAt(LocalDateTime.now());
        projectRepository.saveAndFlush(rmsProject); // flush ensures the lock is useful

        // STEP 6: Update readiness
        try {
            readinessUpdater.updateReadiness(rmsProject);
        } catch (Exception e) {
            // Log the error but don't fail the entire CDC processing
            System.err.println("Failed to update readiness for project " + pmsProjectId + ": " + e.getMessage());
        }
    }
    private void handleDelete(Struct before) {
        if (before == null) return;

        Long pmsProjectId = ((Number) before.get("id")).longValue();

        projectRepository.findById(pmsProjectId)
                .ifPresent(project -> {
                    project.setProjectStatus(ProjectStatus.ARCHIVED);
                    project.setLastSyncedAt(LocalDateTime.now());
                    projectRepository.save(project);
                });
    }

}
