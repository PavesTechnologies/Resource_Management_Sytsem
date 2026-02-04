package com.cdc.listener;

import com.cdc.mapping.ColumnMapping;
import com.cdc.mapping.ProjectCdcMappingRegistry;
import com.cdc.util.CdcValueConverter;
import com.cdc.util.DebeziumChangeDetector;
import com.cdc.util.ReflectionUtil;
import com.entity.Project;
import com.entity_enums.ProjectStatus;
import com.repo.ProjectRepository;
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

        /* =========================
           DELETE → SOFT DELETE
           ========================= */
        if ("d".equals(op)) {
            if (before == null) return;

            Long pmsProjectId =
                    ((Number) before.get("id")).longValue();

            projectRepository.findById(pmsProjectId)
                    .ifPresent(project -> {
                        project.setProjectStatus(ProjectStatus.ARCHIVED);
                        project.setLastSyncedAt(LocalDateTime.now());
                        projectRepository.save(project);
                    });

            return;
        }

        /* =========================
           INSERT / UPDATE
           ========================= */
        if (after == null) return;

        Long pmsProjectId =
                ((Number) after.get("id")).longValue();

        Project rmsProject = projectRepository
                .findById(pmsProjectId)
                .orElseGet(() -> {
                    Project p = new Project();
                    p.setPmsProjectId(pmsProjectId);
                    return p;
                });

        Set<String> changedColumns =
                DebeziumChangeDetector.detectChangedColumns(before, after);

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

        rmsProject.setLastSyncedAt(LocalDateTime.now());
        projectRepository.save(rmsProject);
    }
}
