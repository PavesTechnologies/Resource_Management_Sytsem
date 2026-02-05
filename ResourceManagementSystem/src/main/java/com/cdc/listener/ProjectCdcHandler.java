package com.cdc.listener;

import com.cdc.mapping.ColumnMapping;
import com.cdc.mapping.ProjectCdcMappingRegistry;
import com.cdc.util.CdcValueConverter;
import com.cdc.util.DebeziumChangeDetector;
import com.cdc.util.ReflectionUtil;
import com.entity.project_entities.Project;
import com.entity_enums.project_enums.ProjectStatus;
import com.repo.project_repo.ProjectRepository;
import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
public class ProjectCdcHandler {

    private final ProjectRepository projectRepository;

    public ProjectCdcHandler(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public void handleEvent(RecordChangeEvent<SourceRecord> event) {

        Struct value = (Struct) event.record().value();
        if (value == null) return;

        String op = value.getString("op");

        // Ignore snapshot reads
        if ("r".equals(op)) return;

        Struct before = value.getStruct("before");
        Struct after  = value.getStruct("after");

        // DELETE (soft delete)
        if ("d".equals(op)) {
            if (before == null) return;

            Long pmsProjectId = ((Number) before.get("id")).longValue();

            projectRepository.findById(pmsProjectId)
                    .ifPresent(project -> {
                        project.setProjectStatus(ProjectStatus.ARCHIVED);
                        project.setLastSyncedAt(LocalDateTime.now());
                        projectRepository.save(project);
                    });
            return;
        }

        // INSERT or UPDATE
        if (after == null) return;

        Long pmsProjectId = ((Number) after.get("id")).longValue();

        // ✅ STEP 1: ATOMIC UPSERT (MULTI-INSTANCE SAFE)
        projectRepository.upsertSkeleton(
                pmsProjectId,
                LocalDateTime.now()
        );

        // ✅ STEP 2: SAFE LOAD (row now guaranteed to exist)
        Project rmsProject = projectRepository.findById(pmsProjectId).get();

        // STEP 3: Detect changed columns
        Set<String> changedColumns =
                DebeziumChangeDetector.detectChangedColumns(before, after);

        // STEP 4: Apply mapped changes
        for (String pmsColumn : changedColumns) {

            ColumnMapping mapping =
                    ProjectCdcMappingRegistry.PMS_TO_RMS.get(pmsColumn);

            if (mapping == null) continue;

            Object rawValue =
                    after.schema().field(pmsColumn) != null
                            ? after.get(pmsColumn)
                            : null;

            Object converted =
                    CdcValueConverter.convert(
                            rawValue,
                            mapping.getFieldType(),
                            mapping.getEnumClass()
                    );

            ReflectionUtil.setField(
                    rmsProject,
                    mapping.getRmsField(),
                    converted
            );
        }

        // STEP 5: audit
        rmsProject.setLastSyncedAt(LocalDateTime.now());

        // STEP 6: persist updates
        projectRepository.save(rmsProject);
    }

}
