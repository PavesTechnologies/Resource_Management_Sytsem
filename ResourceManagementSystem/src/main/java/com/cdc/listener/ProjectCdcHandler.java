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
import com.service_imple.availability_service_impl.ProjectTimelineChangeService;
import com.service_imple.project_service_impl.ProjectReadinessUpdaterService;
import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Component
public class ProjectCdcHandler {

    private final ProjectRepository projectRepository;
    private final ProjectReadinessUpdaterService readinessUpdater;
    private final ClientRepo clientRepo;
    private final ProjectTimelineChangeService projectTimelineChangeService;

    public ProjectCdcHandler(ProjectRepository projectRepository, ProjectReadinessUpdaterService readinessUpdater, ClientRepo clientRepo, ProjectTimelineChangeService projectTimelineChangeService) {
        this.projectRepository = projectRepository;
        this.readinessUpdater = readinessUpdater;
        this.clientRepo = clientRepo;
        this.projectTimelineChangeService = projectTimelineChangeService;
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

            Object rawValue = after.schema().field(pmsColumn) != null ? after.get(pmsColumn) : null;
            Object converted = CdcValueConverter.convert(rawValue, mapping.getFieldType(), mapping.getEnumClass());

            try {
                ReflectionUtil.setField(rmsProject, mapping.getRmsField(), converted);
            } catch (Exception e) {
                // Log the error but continue processing other fields
                System.err.println("Failed to set field " + mapping.getRmsField() + " for project " + pmsProjectId + ": " + e.getMessage());
            }
        }

        // STEP 5: Handle special field updates
        boolean isNewProject = (rmsProject.getCreatedAt() == null);
        
        // Update risk_level_updated when risk_level changes
        if (changedColumns.contains("risk_level")) {
            rmsProject.setRiskLevelUpdatedAt(LocalDateTime.now());
        }
        
        // Set createdAt for new projects
        if (isNewProject) {
            rmsProject.setCreatedAt(LocalDateTime.now());
            // Set data status based on mandatory fields
            rmsProject.setDataStatus(calculateDataStatus(rmsProject));
        } else {
            // For existing projects, recalculate data status if relevant fields changed
            if (hasRelevantFieldChanges(changedColumns)) {
                rmsProject.setDataStatus(calculateDataStatus(rmsProject));
            }
        }
        
        // STEP 6: Audit and Persist
        rmsProject.setLastSyncedAt(LocalDateTime.now());
        projectRepository.saveAndFlush(rmsProject); // flush ensures the lock is useful

        // STEP 7: Update readiness
        try {
            readinessUpdater.updateReadiness(rmsProject);
        } catch (Exception e) {
            // Log the error but don't fail the entire CDC processing
            System.err.println("Failed to update readiness for project " + pmsProjectId + ": " + e.getMessage());
        }

        // STEP 8: Handle project timeline changes for availability recalculation
        try {
            if (changedColumns.contains("start_date") || changedColumns.contains("end_date")) {
                LocalDateTime oldStartDate = extractLocalDateTime(before, "start_date");
                LocalDateTime oldEndDate = extractLocalDateTime(before, "end_date");
                LocalDateTime newStartDate = extractLocalDateTime(after, "start_date");
                LocalDateTime newEndDate = extractLocalDateTime(after, "end_date");
                
                projectTimelineChangeService.handleProjectTimelineChange(
                    pmsProjectId, oldStartDate, oldEndDate, newStartDate, newEndDate);
            }
        } catch (Exception e) {
            // Log error but don't fail entire CDC processing
            System.err.println("Failed to handle project timeline change for project " + pmsProjectId + ": " + e.getMessage());
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

    private LocalDateTime extractLocalDateTime(Struct struct, String fieldName) {
        if (struct == null) {
            return null;
        }
        
        Object value = struct.get(fieldName);
        
        if (value == null) return null;
        
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        
        // Handle java.sql.Timestamp
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        }
        
        // Handle java.time.Instant
        if (value instanceof java.time.Instant) {
            return LocalDateTime.ofInstant((java.time.Instant) value, java.time.ZoneId.systemDefault());
        }
        
        // Handle java.util.Date
        if (value instanceof java.util.Date) {
            return new java.sql.Timestamp(((java.util.Date) value).getTime()).toLocalDateTime();
        }
        
        // Handle Long (timestamp)
        if (value instanceof Long) {
            try {
                long timestamp = (Long) value;
                
                // Check timestamp scale and convert accordingly
                if (timestamp > 1_000_000_000_000_000L) { // microseconds (16+ digits)
                    return LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(timestamp / 1_000_000, (timestamp % 1_000_000) * 1000), java.time.ZoneId.systemDefault());
                } else if (timestamp > 1_000_000_000_000L) { // milliseconds (13 digits)
                    return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), java.time.ZoneId.systemDefault());
                } else { // seconds (10 digits)
                    return LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(timestamp), java.time.ZoneId.systemDefault());
                }
            } catch (Exception e) {
                System.err.println("Failed to convert Long timestamp for field " + fieldName + ": " + e.getMessage());
                return null;
            }
        }
        
        // Handle string conversion if needed
        if (value instanceof String) {
            try {
                String strValue = value.toString().trim();
                if (strValue.isEmpty()) return null;
                
                // Try common date formats
                if (strValue.contains("T")) {
                    return LocalDateTime.parse(strValue);
                } else {
                    // Try parsing as date
                    LocalDate date = LocalDate.parse(strValue);
                    return date.atStartOfDay();
                }
            } catch (Exception e) {
                System.err.println("Failed to parse string date for field " + fieldName + ": " + e.getMessage());
                return null;
            }
        }
        
        System.err.println("Unsupported date type for field " + fieldName + ": " + value.getClass().getName());
        return null;
    }

    /**
     * Calculate data status based on mandatory fields
     */
    private com.entity_enums.project_enums.ProjectDataStatus calculateDataStatus(Project project) {
        // Check mandatory fields
        if (project.getName() == null || project.getName().trim().isEmpty()) {
            return com.entity_enums.project_enums.ProjectDataStatus.PENDING;
        }
        
        if (project.getClientId() == null) {
            return com.entity_enums.project_enums.ProjectDataStatus.PENDING;
        }
        
        // Optional: Check other important fields that might be considered mandatory
        if (project.getProjectManagerId() == null) {
            return com.entity_enums.project_enums.ProjectDataStatus.PENDING;
        }
        
        if (project.getStartDate() == null) {
            return com.entity_enums.project_enums.ProjectDataStatus.PENDING;
        }
        
        if (project.getEndDate() == null) {
            return com.entity_enums.project_enums.ProjectDataStatus.PENDING;
        }
        
        // All mandatory fields are present
        return com.entity_enums.project_enums.ProjectDataStatus.COMPLETE;
    }
    
    /**
     * Check if any relevant fields changed that would affect data status
     */
    private boolean hasRelevantFieldChanges(Set<String> changedColumns) {
        return changedColumns.contains("name") ||
               changedColumns.contains("client_id") ||
               changedColumns.contains("project_manager_id") ||
               changedColumns.contains("start_date") ||
               changedColumns.contains("end_date");
    }

}
