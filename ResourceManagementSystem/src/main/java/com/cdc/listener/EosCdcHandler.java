package com.cdc.listener;

import com.cdc.config.EosTrackedColumns;
import com.cdc.derivation.ResourceDerivationService;
import com.cdc.execution.CdcSafeExecutor;
import com.cdc.failure.FailureRecorder;
import com.cdc.isolation.PartialFieldFailureIsolationService;
import com.cdc.mapping.CdcValueConverter;
import com.cdc.mapping.ColumnMapping;
import com.cdc.mapping.EosCdcMappingRegistry;
import com.cdc.protection.StaleEventProtectionService;
import com.cdc.schema.SchemaEvolutionToleranceService;
import com.cdc.service.ResourceService;
import com.cdc.throttling.ReplayThrottlingService;
import com.cdc.util.DebeziumChangeDetector;
import com.cdc.validation.ReplayFreshnessValidationService;
import com.entity.resource_entities.Resource;
import com.repo.resource_repo.ResourceRepository;
import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SELECTIVE CHANGE-DRIVEN EOS → RMS CDC Handler.
 * 
 * Architecture:
 * EOS Binlog → Debezium → Changed Column Detection → Tracked Column Validation
 * → Load/Create Resource → Direct Field Mapping → Derivation Service → Selective Save
 * 
 * Features:
 * - Selective change-driven processing (only tracked columns)
 * - Two-phase processing (direct mapping + derived field calculation)
 * - Minimal DB updates (selective save logic)
 * - Enterprise-grade idempotency and safety
 */
@Component
public class EosCdcHandler {

    private final CdcSafeExecutor cdcSafeExecutor;
    private final FailureRecorder failureRecorder;
    private final ResourceService resourceService;
    private final ResourceDerivationService resourceDerivationService;
    private final ResourceRepository resourceRepository;
    private final StaleEventProtectionService staleEventProtectionService;
    private final ReplayThrottlingService replayThrottlingService;
    private final SchemaEvolutionToleranceService schemaEvolutionToleranceService;
    private final PartialFieldFailureIsolationService partialFieldFailureIsolationService;
    private final ReplayFreshnessValidationService replayFreshnessValidationService;
    private final CdcValueConverter cdcValueConverter;

    public EosCdcHandler(CdcSafeExecutor cdcSafeExecutor, 
                        FailureRecorder failureRecorder,
                        ResourceService resourceService,
                        ResourceDerivationService resourceDerivationService,
                        ResourceRepository resourceRepository,
                        StaleEventProtectionService staleEventProtectionService,
                        ReplayThrottlingService replayThrottlingService,
                        SchemaEvolutionToleranceService schemaEvolutionToleranceService,
                        PartialFieldFailureIsolationService partialFieldFailureIsolationService,
                        ReplayFreshnessValidationService replayFreshnessValidationService,
                        CdcValueConverter cdcValueConverter) {
        this.cdcSafeExecutor = cdcSafeExecutor;
        this.failureRecorder = failureRecorder;
        this.resourceService = resourceService;
        this.resourceDerivationService = resourceDerivationService;
        this.resourceRepository = resourceRepository;
        this.staleEventProtectionService = staleEventProtectionService;
        this.replayThrottlingService = replayThrottlingService;
        this.schemaEvolutionToleranceService = schemaEvolutionToleranceService;
        this.partialFieldFailureIsolationService = partialFieldFailureIsolationService;
        this.replayFreshnessValidationService = replayFreshnessValidationService;
        this.cdcValueConverter = cdcValueConverter;
    }

    /**
     * Main CDC event handler for EOS employee_details table.
     * 
     * Processing Flow:
     * 1. Extract EOS event data
     * 2. Detect changed columns
     * 3. Validate tracked column changes
     * 4. Skip processing if no tracked changes
     * 5. Load/create RMS Resource
     * 6. Apply direct field mappings
     * 7. Populate derived fields
     * 8. Save selectively
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEvent(RecordChangeEvent<SourceRecord> event) {
        Struct value = (Struct) event.record().value();
        if (value == null) return;

        String op = value.getString("op");
        if ("r".equals(op)) return; // Skip snapshot records

        Struct before = value.getStruct("before");
        Struct after = value.getStruct("after");

        // Extract basic information for logging and error handling
        String tableName = extractTableName(event);
        String entityId = extractEntityId(after != null ? after : before);
        String operation = "c".equals(op) ? "CREATE" : "u".equals(op) ? "UPDATE" : "DELETE";
        String payload = event.record().toString();

        // CRITICAL: Schema evolution tolerance processing
        final Struct finalBefore = before;
        final Struct finalAfter = after;
        
        if (before != null) {
            before = schemaEvolutionToleranceService.processSchemaEvolution(tableName, before.schema(), before);
        }
        if (after != null) {
            after = schemaEvolutionToleranceService.processSchemaEvolution(tableName, after.schema(), after);
        }

        // CRITICAL: Replay throttling check to prevent storm conditions
        String entityType = "EOS-" + tableName;
        ReplayThrottlingService.ThrottlingResult throttlingResult = replayThrottlingService.checkReplayAllowed(entityType);
        
        if (!throttlingResult.isAllowed()) {
            System.out.println("REPLAY THROTTLED - Entity: " + entityId + 
                              ", Type: " + entityType + 
                              ", Reason: " + throttlingResult.getReason());
            return; // Skip processing due to throttling
        }

        // CRITICAL: Replay freshness validation
        LocalDateTime eventTimestamp = staleEventProtectionService.extractEventTimestamp(after);
        if (eventTimestamp != null) {
            ReplayFreshnessValidationService.FreshnessValidationResult freshnessResult = 
                replayFreshnessValidationService.validateFreshness(entityType, eventTimestamp, entityId);
            
            if (!freshnessResult.isValid()) {
                System.out.println("REPLAY FRESHNESS VALIDATION FAILED - Entity: " + entityId + 
                                  ", Type: " + entityType + 
                                  ", Reason: " + freshnessResult.getMessage());
                return; // Skip processing due to stale event
            }
        }

        final long startTime = System.currentTimeMillis();
        
        // Execute CDC processing safely using shared infrastructure
        cdcSafeExecutor.execute(
            tableName,
            entityId,
            operation,
            payload,
            () -> {
                try {
                    processEosEvent(tableName, op, finalBefore, finalAfter);
                } finally {
                    // Record processing time for throttling metrics
                    long processingTime = System.currentTimeMillis() - startTime;
                    replayThrottlingService.recordReplayCompletion(entityType, processingTime);
                }
            }
        );
    }

    /**
     * Process EOS CDC events with selective change-driven architecture.
     * 
     * Processing Flow:
     * 1. Validate table (only employee_details)
     * 2. Extract EOS entity ID
     * 3. Detect changed columns
     * 4. Validate tracked column changes
     * 5. Skip processing if no tracked changes
     * 6. Load/create RMS Resource
     * 7. Apply direct field mappings (PHASE 1)
     * 8. Populate derived fields (PHASE 2)
     * 9. Save selectively
     */
    private void processEosEvent(String tableName, String op, Struct before, Struct after) {
        // DELETE handling
        if ("d".equals(op)) {
            handleEosDelete(tableName, before);
            return;
        }

        // INSERT or UPDATE handling
        if (after == null) return;

        // Only process employee_details table for Resource synchronization
        if (!"employee_details".equals(tableName)) {
            System.out.println("Skipping non-employee_details table: " + tableName);
            return;
        }

        // Extract EOS entity ID
        String eosEntityId = extractEosEntityId(tableName, after);
        if (eosEntityId == null) {
            System.err.println("Failed to extract EOS entity ID from table: " + tableName);
            return;
        }

        // STEP 1: Detect changed columns
        Set<String> changedColumns = DebeziumChangeDetector.detectChangedColumns(before, after);
        
        // STEP 2: Validate tracked column changes
        if (!EosTrackedColumns.containsTrackedChanges(changedColumns)) {
            System.out.println("Skipping EOS update - no tracked columns changed for ID: " + eosEntityId);
            return; // Skip processing if no tracked columns changed
        }

        // STEP 3: Filter to tracked changes only
        Set<String> trackedChanges = EosTrackedColumns.filterTrackedColumns(changedColumns);
        
        System.out.println("Processing EOS CDC Event - ID: " + eosEntityId + 
                          ", Tracked Changes: " + trackedChanges.size() + 
                          ", Total Changes: " + changedColumns.size());

        // STEP 4: CRITICAL - Stale Event Protection
        LocalDateTime incomingEventTimestamp = staleEventProtectionService.extractEventTimestamp(after);
        
        // Load existing resource to check timestamp
        Resource existingResource = resourceRepository.findById(eosEntityId).orElse(null);
        if (existingResource != null) {
            if (staleEventProtectionService.isStaleEvent(existingResource.getChangedAt(), incomingEventTimestamp, eosEntityId)) {
                System.out.println("REJECTING STALE EVENT - EOS ID: " + eosEntityId + 
                                  ", RMS Timestamp: " + existingResource.getChangedAt() + 
                                  ", EOS Timestamp: " + incomingEventTimestamp);
                return; // Skip processing stale event
            }
        }

        // STEP 5: Load or create RMS Resource
        Resource resource = loadOrCreateResource(eosEntityId);
        if (resource == null) {
            System.err.println("Failed to load/create Resource for EOS ID: " + eosEntityId);
            return;
        }

        // STEP 6: PHASE 1 - Apply direct field mappings
        boolean hasDirectChanges = applyDirectFieldMappings(resource, trackedChanges, after, tableName);

        // STEP 7: PHASE 2 - Populate derived fields
        resourceService.populateDerivedFields(resource, after);

        // STEP 8: Selective save (only if meaningful changes)
        boolean saved = resourceService.saveResourceSelective(resource);
        
        if (saved) {
            System.out.println("EOS CDC Event Processed Successfully - ID: " + eosEntityId + 
                              ", Direct Changes: " + hasDirectChanges + 
                              ", Timestamp: " + LocalDateTime.now());
        } else {
            System.out.println("EOS CDC Event Skipped (No meaningful changes) - ID: " + eosEntityId);
        }
    }

    /**
     * Load or create RMS Resource entity.
     * 
     * @param eosEntityId The EOS employee_id to use as resourceId
     * @return Resource entity or null if failed
     */
    private Resource loadOrCreateResource(String eosEntityId) {
        try {
            // Try to load existing Resource
            Resource resource = resourceService.loadResource(eosEntityId);
            
            if (resource == null) {
                // Create new Resource if not found
                resource = resourceService.createResource(eosEntityId);
                System.out.println("Created new Resource for EOS ID: " + eosEntityId);
            }
            
            return resource;
        } catch (Exception e) {
            System.err.println("Failed to load/create Resource for EOS ID " + eosEntityId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Apply direct field mappings to Resource entity with PARTIAL FIELD FAILURE ISOLATION.
     * 
     * @param resource The Resource entity to update
     * @param trackedChanges Set of tracked changed columns
     * @param after The EOS after payload
     * @param tableName Source table name for schema evolution
     * @return true if any field was updated, false otherwise
     */
    private boolean applyDirectFieldMappings(Resource resource, Set<String> trackedChanges, Struct after, String tableName) {
        boolean hasChanges = false;
        String entityType = "EOS-" + tableName;
        
        // Create field processing requests for batch isolation
        List<PartialFieldFailureIsolationService.FieldProcessingRequest> fieldRequests = new ArrayList<>();
        
        for (String eosColumn : trackedChanges) {
            ColumnMapping mapping = EosCdcMappingRegistry.getMapping(eosColumn);
            if (mapping == null) continue;

            // Create field operation with isolation
            PartialFieldFailureIsolationService.FieldOperation operation = () -> {
                // CRITICAL: Use schema evolution tolerance for field extraction
                Object rawValue = schemaEvolutionToleranceService.getFieldValueWithTolerance(after, eosColumn, tableName);
                Object converted = cdcValueConverter.convert(rawValue, mapping.getFieldType(), mapping.getEnumClass(), eosColumn);
                
                boolean fieldUpdated = resourceService.applyDirectMapping(resource, mapping.getRmsField(), converted);
                if (fieldUpdated) {
                    System.out.println("Applied Direct Mapping - EOS: " + eosColumn + 
                                      " → RMS: " + mapping.getRmsField() + 
                                      " = " + converted);
                }
                return fieldUpdated;
            };

            fieldRequests.add(PartialFieldFailureIsolationService.FieldProcessingRequest.builder()
                    .fieldName(eosColumn)
                    .operation(operation)
                    .build());
        }

        // Process fields with batch isolation
        if (!fieldRequests.isEmpty()) {
            PartialFieldFailureIsolationService.BatchFieldProcessingResult batchResult = 
                partialFieldFailureIsolationService.processFieldsWithBatchIsolation(
                    fieldRequests, entityType, String.valueOf(resource.getResourceId()));

            // Analyze results
            if (batchResult.isCompleteSuccess()) {
                hasChanges = true;
                System.out.println("All " + batchResult.getSuccessCount() + " field mappings processed successfully");
            } else if (batchResult.isPartialSuccess()) {
                hasChanges = true; // Some fields succeeded
                System.out.println("Partial success: " + batchResult.getSuccessCount() + " succeeded, " + 
                                  batchResult.getFailureCount() + " failed for entity " + batchResult.getEntityId());
                
                // Log failed fields
                for (PartialFieldFailureIsolationService.FieldProcessingResult result : batchResult.getResults()) {
                    if (!result.isSuccess()) {
                        System.err.println("Field mapping failed: " + result.getFieldName() + 
                                         " - Error: " + result.getErrorMessage());
                    }
                }
            } else {
                System.err.println("Complete failure: All " + batchResult.getFailureCount() + 
                                 " field mappings failed for entity " + batchResult.getEntityId());
            }
        }
        
        return hasChanges;
    }

    /**
     * Handle EOS entity deletion.
     * Follows the same soft-delete pattern as PmsCdcHandler.
     */
    private void handleEosDelete(String tableName, Struct before) {
        if (before == null) return;

        String eosEntityId = extractEosEntityId(tableName, before);
        if (eosEntityId == null) return;

        System.out.println("EOS CDC Delete - Table: " + tableName + ", ID: " + eosEntityId);
        
        // Enterprise-safe soft-delete (REUSED pattern from PmsCdcHandler)
        Resource resource = resourceRepository.findById(eosEntityId).orElse(null);
        if (resource == null) {
            System.err.println("Resource not found for soft-delete: " + eosEntityId);
            return;
        }

        // Soft-delete using same pattern as PMS CDC
        resource.setActiveFlag(false);
        resource.setEmploymentStatus(com.entity_enums.resource_enums.EmploymentStatus.EXITED);
        resource.setDateOfExit(java.time.LocalDate.now());
        resource.setChangedAt(java.time.LocalDateTime.now());
        
        try {
            resourceRepository.save(resource);
            System.out.println("EOS CDC Delete Processed Successfully - ID: " + eosEntityId);
        } catch (Exception e) {
            System.err.println("Failed to soft-delete resource " + eosEntityId + ": " + e.getMessage());
        }
    }

    /**
     * Extract EOS entity ID from employee_details table.
     * Only handles employee_details for Resource synchronization.
     */
    private String extractEosEntityId(String tableName, Struct struct) {
        if (struct == null) return null;

        // Only handle employee_details table for Resource synchronization
        if (!"employee_details".equals(tableName)) {
            return null;
        }

        Object idValue = struct.get("employee_id");
        if (idValue != null) {
            return idValue.toString();
        }
        
        return null;
    }

    /**
     * Extract table name from the Debezium event (SHARED with PMS pattern).
     */
    private String extractTableName(RecordChangeEvent<SourceRecord> event) {
        Struct value = (Struct) event.record().value();
        Struct source = value != null ? value.getStruct("source") : null;
        return source != null ? source.getString("table") : "unknown";
    }

    /**
     * Extract entity ID from Debezium struct (SHARED with PMS pattern).
     */
    private String extractEntityId(Struct struct) {
        if (struct == null) return "unknown";
        Object id = struct.get("id");
        return id != null ? id.toString() : "unknown";
    }

    /**
     * Extract LocalDateTime from Debezium struct.
     * Reuses the same logic as PmsCdcHandler for consistency.
     */
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
}
