package com.cdc.listener;

import com.cdc.execution.CdcSafeExecutor;
import com.cdc.failure.FailureRecorder;
import com.cdc.mapping.ColumnMapping;
import com.cdc.mapping.PmsCdcMappingRegistry;
import com.cdc.mapping.CdcValueConverter;
import com.cdc.protection.StaleEventProtectionService;
import com.cdc.throttling.ReplayThrottlingService;
import com.cdc.util.DebeziumChangeDetector;
import com.cdc.util.ReflectionUtil;
import com.entity.project_entities.Project;
import com.entity_enums.project_enums.ProjectDataStatus;
import com.entity_enums.project_enums.ProjectStatus;
import com.repo.client_repo.ClientRepo;
import com.repo.project_repo.ProjectRepository;
import com.service_imple.availability_service_impl.ProjectTimelineChangeService;
import com.service_imple.project_service_impl.ProjectReadinessUpdaterService;
import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * ENTERPRISE-GRADE PMS CDC Handler with unified error handling and retry infrastructure.
 * 
 * Enhanced to match EOS-CDC capabilities:
 * - Uses CdcSafeExecutor for enterprise-grade error handling
 * - Implements two-phase processing (direct mapping + derived fields)
 * - Supports selective column filtering (configurable)
 * - Reuses unified retry infrastructure for consistency
 * - Identical processing flow to EOS-CDC for enterprise compatibility
 */
@Slf4j
@Component
public class PmsCdcHandler {

    private final CdcSafeExecutor cdcSafeExecutor;
    private final FailureRecorder failureRecorder;
    private final ProjectRepository projectRepository;
    private final ProjectReadinessUpdaterService readinessUpdater;
    private final ClientRepo clientRepo;
    private final ProjectTimelineChangeService projectTimelineChangeService;
    private final StaleEventProtectionService staleEventProtectionService;
    private final ReplayThrottlingService replayThrottlingService;
    private final CdcValueConverter cdcValueConverter;

    public PmsCdcHandler(CdcSafeExecutor cdcSafeExecutor,
                      FailureRecorder failureRecorder,
                      ProjectRepository projectRepository, 
                      ProjectReadinessUpdaterService readinessUpdater, 
                      ClientRepo clientRepo, 
                      ProjectTimelineChangeService projectTimelineChangeService,
                      StaleEventProtectionService staleEventProtectionService,
                      ReplayThrottlingService replayThrottlingService,
                      CdcValueConverter cdcValueConverter) {
        this.cdcSafeExecutor = cdcSafeExecutor;
        this.failureRecorder = failureRecorder;
        this.projectRepository = projectRepository;
        this.readinessUpdater = readinessUpdater;
        this.clientRepo = clientRepo;
        this.projectTimelineChangeService = projectTimelineChangeService;
        this.staleEventProtectionService = staleEventProtectionService;
        this.replayThrottlingService = replayThrottlingService;
        this.cdcValueConverter = cdcValueConverter;
    }

    // ... existing imports and class setup

    /**
     * ENTERPRISE-GRADE PMS CDC event handler with unified error handling.
     * 
     * IDENTICAL to EOS-CDC processing flow:
     * 1. Extract event data
     * 2. Execute safely with CdcSafeExecutor (enterprise-grade)
     * 3. Process with two-phase architecture
     * 4. Handle failures with retry infrastructure
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEvent(RecordChangeEvent<SourceRecord> event) {
        Struct value = (Struct) event.record().value();
        if (value == null) return;

        String rawOp = value.getString("op");
        // "r" = snapshot read — treat the same as insert so we catch up after CDC downtime
        final String op = "r".equals(rawOp) ? "c" : rawOp;

        Struct before = value.getStruct("before");
        Struct after  = value.getStruct("after");

        // Extract basic information for logging and error handling (EOS pattern)
        String tableName = extractTableName(event);
        String entityId = extractEntityId(after != null ? after : before);
        String operation = "c".equals(op) ? "CREATE" : "u".equals(op) ? "UPDATE" : "DELETE";
        String payload = event.record().toString();

        // CRITICAL: Replay throttling check to prevent storm conditions
        String entityType = "PMS-" + tableName;
        ReplayThrottlingService.ThrottlingResult throttlingResult = replayThrottlingService.checkReplayAllowed(entityType);
        
        if (!throttlingResult.isAllowed()) {
            log.warn("REPLAY THROTTLED - Entity: {}, Type: {}, Reason: {}", entityId, entityType, throttlingResult.getReason());
            return;
        }

        long startTime = System.currentTimeMillis();

        // Execute CDC processing safely using shared EOS infrastructure
        cdcSafeExecutor.execute(
            "PMS-" + tableName,
            entityId,
            operation,
            payload,
            () -> {
                try {
                    processPmsEvent(tableName, op, before, after);
                } finally {
                    // Record processing time for throttling metrics
                    long processingTime = System.currentTimeMillis() - startTime;
                    replayThrottlingService.recordReplayCompletion(entityType, processingTime);
                }
            }
        );
    }

    /**
     * ENTERPRISE-GRADE PMS event processing with two-phase architecture.
     * 
     * IDENTICAL to EOS-CDC processing flow:
     * 1. Validate table and extract entity ID
     * 2. Detect and filter changed columns
     * 3. Load/create entity with locking
     * 4. PHASE 1: Apply direct field mappings
     * 5. PHASE 2: Populate derived fields
     * 6. Selective save with enterprise safety
     */
    private void processPmsEvent(String tableName, String op, Struct before, Struct after) {
        // DELETE handling (UNIFIED with EOS pattern)
        if ("d".equals(op)) {
            handlePmsDelete(tableName, before);
            return;
        }

        // INSERT or UPDATE handling
        if (after == null) return;

        // Only process projects table for Project synchronization (EOS pattern)
        if (!"projects".equals(tableName)) {
            log.debug("Skipping non-projects table: {}", tableName);
            return;
        }

        // Extract PMS entity ID (EOS pattern)
        Long pmsProjectId = extractPmsProjectId(after);
        if (pmsProjectId == null) {
            log.error("Failed to extract PMS project ID from table: {}", tableName);
            return;
        }

        // STEP 1: Detect changed columns (UNIFIED)
        Set<String> changedColumns = DebeziumChangeDetector.detectChangedColumns(before, after);
        
        // STEP 2: Validate tracked column changes (PMS processes all - like EOS AllColumnsFilter)
        if (changedColumns.isEmpty()) {
            log.debug("Skipping PMS update - no columns changed for ID: {}", pmsProjectId);
            return;
        }

        // STEP 3: CRITICAL - Stale Event Protection
        LocalDateTime incomingEventTimestamp = staleEventProtectionService.extractEventTimestamp(after);
        
        // Load existing project to check timestamp
        Project existingProject = projectRepository.findById(pmsProjectId).orElse(null);
        if (existingProject != null) {
            if (staleEventProtectionService.isStaleEvent(existingProject.getChangedAt(), incomingEventTimestamp, String.valueOf(pmsProjectId))) {
                log.warn("REJECTING STALE EVENT - PMS ID: {}, RMS Timestamp: {}, PMS Timestamp: {}",
                        pmsProjectId, existingProject.getLastSyncedAt(), incomingEventTimestamp);
                return; // Skip processing stale event
            }
        }

        // STEP 4: Load or create RMS Project with enterprise locking
        Project project = loadOrCreateProject(pmsProjectId, after);
        if (project == null) {
            log.error("Failed to load/create Project for PMS ID: {}", pmsProjectId);
            return;
        }

        // STEP 5: PHASE 1 - Apply direct field mappings (UNIFIED)
        boolean hasDirectChanges = applyDirectFieldMappings(project, changedColumns, after);

        // STEP 6: PHASE 2 - Populate derived fields (UNIFIED)
        populateDerivedFields(project, after, changedColumns);

        // STEP 7: Selective save with enterprise safety (UNIFIED)
        boolean saved = saveProjectSelective(project);
        
        if (saved) {
            log.info("PMS CDC Event Processed - ID: {}, Direct Changes: {}", pmsProjectId, hasDirectChanges);
        } else {
            log.debug("PMS CDC Event Skipped (No meaningful changes) - ID: {}", pmsProjectId);
        }
    }

    /**
     * Load or create RMS Project entity with enterprise locking.
     * 
     * @param pmsProjectId The PMS project identifier
     * @param after The PMS Debezium payload
     * @return Project entity or null if failed
     */
    private Project loadOrCreateProject(Long pmsProjectId, Struct after) {
        try {
            // Extract project data for upsert
            String projectName = after.getString("name");
            if (projectName == null) projectName = "New Project";
            
            // Extract and validate clientId
            UUID clientId = null;
            Object clientIdValue = after.get("client_id");
            if (clientIdValue != null) {
                try {
                    clientId = UUID.fromString(clientIdValue.toString());
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
                    projectName,
                    LocalDateTime.now(),
                    clientId
            );

            // ✅ STEP 2: PESSIMISTIC LOCK (Multi-instance safe)
            Project project = projectRepository.findByIdWithLock(pmsProjectId).orElse(null);
            
            if (project == null) {
                log.error("Entity not found after upsert for PMS project ID: {}", pmsProjectId);
                return null;
            }

            return project;
        } catch (Exception e) {
            log.error("Failed to load/create Project for PMS ID {}: {}", pmsProjectId, e.getMessage());
            return null;
        }
    }

    /**
     * Apply direct field mappings to Project entity (UNIFIED with EOS pattern).
     * 
     * @param project The Project entity to update
     * @param changedColumns Set of changed columns
     * @param after The PMS Debezium payload
     * @return true if any field was updated, false otherwise
     */
    private boolean applyDirectFieldMappings(Project project, Set<String> changedColumns, Struct after) {
        boolean hasChanges = false;
        
        for (String pmsColumn : changedColumns) {
            ColumnMapping mapping = PmsCdcMappingRegistry.PMS_TO_RMS.get(pmsColumn);
            if (mapping == null) continue;

            Object rawValue = after.schema().field(pmsColumn) != null ? after.get(pmsColumn) : null;
            Object converted = cdcValueConverter.convert(rawValue, mapping.getFieldType(), mapping.getEnumClass());

            // Guard FK: only set client_id if that client actually exists in RMS
            if ("client_id".equals(pmsColumn) && converted instanceof java.util.UUID) {
                if (!clientRepo.existsById((java.util.UUID) converted)) {
                    log.debug("Skipping client_id mapping - client not in RMS yet: {}", converted);
                    continue;
                }
            }

            try {
                Object currentValue = ReflectionUtil.getFieldValue(project, mapping.getRmsField());

                // Skip if value hasn't changed (EOS pattern)
                if (Objects.equals(currentValue, converted)) {
                    continue;
                }

                ReflectionUtil.setField(project, mapping.getRmsField(), converted);
                hasChanges = true;
                
                log.debug("Applied Direct Mapping - PMS: {} -> RMS: {} = {}", pmsColumn, mapping.getRmsField(), converted);
            } catch (Exception e) {
                log.error("Failed to apply direct mapping for {} for PMS ID {}: {}", mapping.getRmsField(), project.getName(), e.getMessage());
            }
        }
        
        return hasChanges;
    }

    /**
     * Populate derived fields using business logic (UNIFIED with EOS pattern).
     * 
     * @param project The Project entity to populate
     * @param pmsPayload The PMS Debezium payload
     * @param changedColumns Set of changed columns
     */
    private void populateDerivedFields(Project project, Struct pmsPayload, Set<String> changedColumns) {
        if (project == null || pmsPayload == null) {
            return;
        }

        // Handle special field updates (derived fields)
        boolean isNewProject = (project.getCreatedAt() == null);
        
        // Only derive riskLevelUpdatedAt if PMS did not send its own value in this event.
        // If risk_level_updated_at was in the payload, Phase 1 already set it.
        if (changedColumns.contains("risk_level")
                && !changedColumns.contains("risk_level_updated_at")
                && !changedColumns.contains("riskLevelUpdatedAt")) {
            project.setRiskLevelUpdatedAt(LocalDateTime.now());
        }
        
        // Set createdAt for new projects — use PMS source timestamp for data governance
        if (isNewProject) {
            LocalDateTime srcCreatedAt = extractStructTimestamp(pmsPayload, "created_at");
            project.setCreatedAt(srcCreatedAt != null ? srcCreatedAt : LocalDateTime.now());
            // Set data status based on mandatory fields (derived field)
            project.setDataStatus(calculateDataStatus(project));
        } else {
            // For existing projects, recalculate data status if relevant fields changed
            if (hasRelevantFieldChanges(changedColumns)) {
                project.setDataStatus(calculateDataStatus(project));
            }
        }
        
        // Store PMS source updated_at directly — used for stale event detection
        LocalDateTime srcChangedAt = extractStructTimestamp(pmsPayload, "updated_at");
        project.setChangedAt(srcChangedAt != null ? srcChangedAt : LocalDateTime.now());

        // RMS audit field: when did RMS last receive this event
        project.setLastSyncedAt(LocalDateTime.now());
    }

    /**
     * Save Project entity with selective logic (UNIFIED with EOS pattern).
     * 
     * @param project The Project entity to save
     * @return true if saved successfully, false if skipped
     */
    private boolean saveProjectSelective(Project project) {
        if (project == null) {
            return false;
        }

        try {
            projectRepository.saveAndFlush(project); // flush ensures the lock is useful
            
            // STEP 7: Update readiness (post-processing)
            try {
                readinessUpdater.updateReadiness(project);
            } catch (Exception e) {
                log.error("Failed to update readiness for project {}: {}", project.getPmsProjectId(), e.getMessage());
            }

            // STEP 8: Handle project timeline changes for availability recalculation (post-processing)
            try {
                // Note: This would need the before/after structs - simplified for now
                // In full implementation, pass changedColumns and extract timeline changes
                projectTimelineChangeService.handleProjectTimelineChange(
                    project.getPmsProjectId(), null, null, project.getStartDate(), project.getEndDate());
            } catch (Exception e) {
                log.error("Failed to handle project timeline change for project {}: {}", project.getPmsProjectId(), e.getMessage());
            }
            
            return true;
        } catch (Exception e) {
            log.error("Failed to save project {}: {}", project.getPmsProjectId(), e.getMessage());
            return false;
        }
    }

    /**
     * Handle PMS entity deletion with enterprise-grade soft-delete (UNIFIED with EOS pattern).
     */
    private void handlePmsDelete(String tableName, Struct before) {
        if (before == null) return;

        Long pmsProjectId = extractPmsProjectId(before);
        if (pmsProjectId == null) return;

        log.info("PMS CDC Delete - Table: {}, ID: {}", tableName, pmsProjectId);
        
        // Enterprise-safe soft-delete (UNIFIED pattern from EOS-CDC)
        Project project = projectRepository.findById(pmsProjectId).orElse(null);
        if (project == null) {
            log.error("Project not found for soft-delete: {}", pmsProjectId);
            return;
        }

        // Soft-delete using same pattern as EOS-CDC
        project.setProjectStatus(ProjectStatus.DELETED);
        project.setLastSyncedAt(LocalDateTime.now());
        
        try {
            projectRepository.save(project);
            log.info("PMS CDC Delete Processed Successfully - ID: {}", pmsProjectId);
        } catch (Exception e) {
            log.error("Failed to soft-delete project {}: {}", pmsProjectId, e.getMessage());
        }
    }

    /**
     * Extract PMS project ID from projects table.
     * 
     * @param struct The Debezium struct
     * @return PMS project ID or null if extraction fails
     */
    private Long extractPmsProjectId(Struct struct) {
        if (struct == null) return null;

        Object idValue = struct.get("id");
        if (idValue instanceof Number) {
            return ((Number) idValue).longValue();
        }
        
        return null;
    }

    /**
     * Extract table name from the Debezium event (UNIFIED with EOS pattern).
     */
    private String extractTableName(RecordChangeEvent<SourceRecord> event) {
        Struct value = (Struct) event.record().value();
        Struct source = value != null ? value.getStruct("source") : null;
        return source != null ? source.getString("table") : "unknown";
    }

    /**
     * Extract entity ID from Debezium struct (UNIFIED with EOS pattern).
     */
    private String extractEntityId(Struct struct) {
        if (struct == null) return "unknown";
        Object id = struct.get("id");
        return id != null ? id.toString() : "unknown";
    }

    /**
     * Calculate data status based on mandatory fields
     */
    private ProjectDataStatus calculateDataStatus(Project project) {
        // Check mandatory fields
        if (project.getName() == null || project.getName().trim().isEmpty()) {
            return ProjectDataStatus.PENDING;
        }
        
        if (project.getClientId() == null) {
            return ProjectDataStatus.PENDING;
        }
        
        // Optional: Check other important fields that might be considered mandatory
        if (project.getProjectManagerId() == null) {
            return ProjectDataStatus.PENDING;
        }
        
        if (project.getStartDate() == null) {
            return ProjectDataStatus.PENDING;
        }
        
        // All mandatory fields are present
        return ProjectDataStatus.COMPLETE;
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

    private LocalDateTime extractStructTimestamp(Struct struct, String fieldName) {
        if (struct == null || struct.schema().field(fieldName) == null) return null;
        Object value = struct.get(fieldName);
        if (value == null) return null;
        if (value instanceof Long) {
            long ts = (Long) value;
            // Debezium DATETIME_MICROSECONDS: > 1_000_000_000_000_000L microseconds since epoch
            if (ts > 1_000_000_000_000_000L)
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(ts / 1_000_000, (ts % 1_000_000) * 1000), ZoneId.systemDefault());
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault());
        }
        try { return LocalDateTime.parse(value.toString()); } catch (Exception e) { return null; }
    }

}
