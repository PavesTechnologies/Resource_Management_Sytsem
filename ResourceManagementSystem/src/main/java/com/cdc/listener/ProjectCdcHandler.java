package com.cdc.listener;

import com.cdc.mapping.ColumnMapping;
import com.cdc.mapping.ProjectCdcMappingRegistry;
import com.cdc.util.CdcValueConverter;
import com.cdc.util.DebeziumChangeDetector;
import com.cdc.util.ReflectionUtil;
import com.entity.project_entities.Project;
import com.entity_enums.project_enums.ProjectStatus;
import com.repo.project_repo.ProjectRepository;
import com.service_imple.project_service_impl.ProjectReadinessUpdaterService;
import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
public class ProjectCdcHandler {

    private final ProjectRepository projectRepository;
    private final ProjectReadinessUpdaterService readinessUpdater;

    public ProjectCdcHandler(ProjectRepository projectRepository, ProjectReadinessUpdaterService readinessUpdater) {
        this.projectRepository = projectRepository;
        this.readinessUpdater = readinessUpdater;
    }

    // ... existing imports and class setup

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

        // ✅ STEP 1: ATOMIC UPSERT (Thread-safe at DB level)
        projectRepository.upsertSkeleton(
                pmsProjectId,
                projectName != null ? projectName : "New Project",
                LocalDateTime.now()
        );

        // ✅ STEP 2: PESSIMISTIC LOCK (Multi-instance safe)
        // This blocks other instances from editing this specific row until this method finishes
        Project rmsProject = projectRepository.findByIdWithLock(pmsProjectId)
                .orElseThrow(() -> new RuntimeException("Entity not found after upsert"));

        // STEP 3: Detect changed columns
        Set<String> changedColumns = DebeziumChangeDetector.detectChangedColumns(before, after);

        // STEP 4: Apply mapped changes
        for (String pmsColumn : changedColumns) {
            ColumnMapping mapping = ProjectCdcMappingRegistry.PMS_TO_RMS.get(pmsColumn);
            if (mapping == null) continue;

            Object rawValue = after.schema().field(pmsColumn) != null ? after.get(pmsColumn) : null;
            Object converted = CdcValueConverter.convert(rawValue, mapping.getFieldType(), mapping.getEnumClass());

            ReflectionUtil.setField(rmsProject, mapping.getRmsField(), converted);
        }

        // STEP 5: Audit and Persist
        rmsProject.setLastSyncedAt(LocalDateTime.now());
        projectRepository.saveAndFlush(rmsProject); // flush ensures the lock is useful

        // STEP 6: Update readiness
        readinessUpdater.updateReadiness(rmsProject);
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
