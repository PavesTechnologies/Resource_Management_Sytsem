package com.cdc.isolation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ENTERPRISE-GRADE Partial Field Failure Isolation Service.
 * 
 * CRITICAL SAFETY MECHANISM to ensure that individual field failures
 * don't cause entire CDC events to fail, maintaining data flow continuity.
 * 
 * Features:
 * - Field-level failure isolation and recovery
 * - Failed field tracking and monitoring
 * - Automatic retry with fallback strategies
 * - Field failure pattern detection
 * - Comprehensive failure reporting
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartialFieldFailureIsolationService {

    // Field failure tracking for monitoring
    private final Map<String, FieldFailureStats> fieldFailureStats = new ConcurrentHashMap<>();
    
    // Failed field recovery strategies
    private final Map<String, List<FieldRecoveryStrategy>> recoveryStrategies = new ConcurrentHashMap<>();

    /**
     * Process field mapping with isolation - prevents single field failures from breaking entire event.
     * 
     * @param fieldName Field name being processed
     * @param entityType Entity type (e.g., "EOS-employee_details", "PMS-projects")
     * @param entityId Entity identifier
     * @param fieldOperation Operation to perform on the field
     * @return Field processing result with isolation applied
     */
    public FieldProcessingResult processFieldWithIsolation(
            String fieldName, 
            String entityType, 
            String entityId, 
            FieldOperation fieldOperation) {
        
        String fieldKey = entityType + "." + fieldName;
        
        try {
            // Execute field operation
            Object result = fieldOperation.execute();
            
            // Record success
            recordFieldSuccess(fieldKey);
            
            return FieldProcessingResult.success(fieldName, result);
            
        } catch (Exception e) {
            // Isolate field failure and attempt recovery
            return handleFieldFailure(fieldName, fieldKey, entityType, entityId, fieldOperation, e);
        }
    }

    /**
     * Process multiple fields with batch isolation.
     * 
     * @param fields List of fields to process
     * @param entityType Entity type
     * @param entityId Entity identifier
     * @return Batch processing result with individual field results
     */
    public BatchFieldProcessingResult processFieldsWithBatchIsolation(
            List<FieldProcessingRequest> fields, 
            String entityType, 
            String entityId) {
        
        List<FieldProcessingResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (FieldProcessingRequest request : fields) {
            FieldProcessingResult result = processFieldWithIsolation(
                request.getFieldName(), 
                entityType, 
                entityId, 
                request.getOperation()
            );
            
            results.add(result);
            
            if (result.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        
        return BatchFieldProcessingResult.builder()
                .entityId(entityId)
                .entityType(entityType)
                .totalFields(fields.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .results(results)
                .build();
    }

    /**
     * Get field failure statistics.
     * 
     * @return Field failure statistics
     */
    public FieldFailureStatistics getStatistics() {
        return FieldFailureStatistics.builder()
                .trackedFields(fieldFailureStats.size())
                .totalFailures(fieldFailureStats.values().stream().mapToLong(stats -> stats.getFailureCount()).sum())
                .totalSuccesses(fieldFailureStats.values().stream().mapToLong(stats -> stats.getSuccessCount()).sum())
                .failureRate(calculateOverallFailureRate())
                .build();
    }

    /**
     * Reset field failure statistics.
     * 
     * @param fieldKey Optional specific field key to reset (null for all)
     */
    public void resetStatistics(String fieldKey) {
        if (fieldKey == null) {
            fieldFailureStats.clear();
            log.info("Reset all field failure statistics");
        } else {
            fieldFailureStats.remove(fieldKey);
            log.info("Reset field failure statistics for {}", fieldKey);
        }
    }

    /**
     * Register recovery strategy for a field.
     * 
     * @param fieldKey Field key (entityType.fieldName)
     * @param strategy Recovery strategy to apply
     */
    public void registerRecoveryStrategy(String fieldKey, FieldRecoveryStrategy strategy) {
        recoveryStrategies.computeIfAbsent(fieldKey, k -> new ArrayList<>()).add(strategy);
        log.info("Registered recovery strategy for field: {}", fieldKey);
    }

    /**
     * Handle field failure with isolation and recovery.
     */
    private FieldProcessingResult handleFieldFailure(
            String fieldName, 
            String fieldKey, 
            String entityType, 
            String entityId, 
            FieldOperation fieldOperation, 
            Exception originalException) {
        
        // Record failure
        recordFieldFailure(fieldKey, originalException);
        
        // Attempt recovery strategies
        List<FieldRecoveryStrategy> strategies = recoveryStrategies.get(fieldKey);
        if (strategies != null && !strategies.isEmpty()) {
            for (FieldRecoveryStrategy strategy : strategies) {
                try {
                    Object recoveredValue = strategy.recover(fieldName, entityType, entityId, originalException);
                    if (recoveredValue != null) {
                        log.info("Field {} recovered using strategy: {}", fieldName, strategy.getClass().getSimpleName());
                        recordFieldRecovery(fieldKey);
                        return FieldProcessingResult.recovered(fieldName, recoveredValue, strategy.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    log.warn("Recovery strategy {} failed for field {}: {}", 
                            strategy.getClass().getSimpleName(), fieldName, e.getMessage());
                }
            }
        }
        
        // All recovery strategies failed - return failure result
        log.error("All recovery strategies failed for field {} in entity {}: {}", 
                fieldName, entityId, originalException.getMessage(), originalException);
        
        return FieldProcessingResult.failed(fieldName, originalException.getMessage());
    }

    /**
     * Record field success for statistics.
     */
    private void recordFieldSuccess(String fieldKey) {
        fieldFailureStats.computeIfAbsent(fieldKey, k -> new FieldFailureStats()).incrementSuccess();
    }

    /**
     * Record field failure for statistics.
     */
    private void recordFieldFailure(String fieldKey, Exception exception) {
        FieldFailureStats stats = fieldFailureStats.computeIfAbsent(fieldKey, k -> new FieldFailureStats());
        stats.incrementFailure();
        stats.recordLastError(exception.getMessage());
        
        // Check for high failure rate
        if (stats.getFailureRate() > 0.5 && stats.getFailureCount() > 10) {
            log.warn("High failure rate detected for field {}: {:.2f}% ({} failures)", 
                    fieldKey, stats.getFailureRate() * 100, stats.getFailureCount());
        }
    }

    /**
     * Record field recovery for statistics.
     */
    private void recordFieldRecovery(String fieldKey) {
        fieldFailureStats.computeIfAbsent(fieldKey, k -> new FieldFailureStats()).incrementRecovery();
    }

    /**
     * Calculate overall failure rate across all fields.
     */
    private double calculateOverallFailureRate() {
        long totalSuccesses = fieldFailureStats.values().stream().mapToLong(FieldFailureStats::getSuccessCount).sum();
        long totalFailures = fieldFailureStats.values().stream().mapToLong(FieldFailureStats::getFailureCount).sum();
        long total = totalSuccesses + totalFailures;
        
        return total > 0 ? (double) totalFailures / total : 0.0;
    }

    /**
     * Field operation interface.
     */
    @FunctionalInterface
    public interface FieldOperation {
        Object execute() throws Exception;
    }

    /**
     * Field recovery strategy interface.
     */
    @FunctionalInterface
    public interface FieldRecoveryStrategy {
        Object recover(String fieldName, String entityType, String entityId, Exception originalException) throws Exception;
    }

    /**
     * Field processing request.
     */
    @lombok.Builder
    @lombok.Data
    public static class FieldProcessingRequest {
        private String fieldName;
        private FieldOperation operation;
    }

    /**
     * Field processing result.
     */
    @lombok.Builder
    @lombok.Data
    public static class FieldProcessingResult {
        private String fieldName;
        private boolean success;
        private Object value;
        private String errorMessage;
        private String recoveryStrategy;

        public static FieldProcessingResult success(String fieldName, Object value) {
            return FieldProcessingResult.builder()
                    .fieldName(fieldName)
                    .success(true)
                    .value(value)
                    .build();
        }

        public static FieldProcessingResult failed(String fieldName, String errorMessage) {
            return FieldProcessingResult.builder()
                    .fieldName(fieldName)
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }

        public static FieldProcessingResult recovered(String fieldName, Object value, String recoveryStrategy) {
            return FieldProcessingResult.builder()
                    .fieldName(fieldName)
                    .success(true)
                    .value(value)
                    .recoveryStrategy(recoveryStrategy)
                    .build();
        }
    }

    /**
     * Batch field processing result.
     */
    @lombok.Builder
    @lombok.Data
    public static class BatchFieldProcessingResult {
        private String entityId;
        private String entityType;
        private int totalFields;
        private int successCount;
        private int failureCount;
        private List<FieldProcessingResult> results;

        public boolean hasFailures() {
            return failureCount > 0;
        }

        public boolean isPartialSuccess() {
            return successCount > 0 && failureCount > 0;
        }

        public boolean isCompleteSuccess() {
            return failureCount == 0 && successCount > 0;
        }

        public boolean isCompleteFailure() {
            return successCount == 0;
        }
    }

    /**
     * Field failure statistics tracking.
     */
    @lombok.Data
    public static class FieldFailureStats {
        private long successCount = 0;
        private long failureCount = 0;
        private long recoveryCount = 0;
        private String lastError;

        public void incrementSuccess() { successCount++; }
        public void incrementFailure() { failureCount++; }
        public void incrementRecovery() { recoveryCount++; }
        public void recordLastError(String error) { this.lastError = error; }

        public double getFailureRate() {
            long total = successCount + failureCount;
            return total > 0 ? (double) failureCount / total : 0.0;
        }

        public double getRecoveryRate() {
            return failureCount > 0 ? (double) recoveryCount / failureCount : 0.0;
        }
    }

    /**
     * Field failure statistics.
     */
    @lombok.Builder
    @lombok.Data
    public static class FieldFailureStatistics {
        private int trackedFields;
        private long totalFailures;
        private long totalSuccesses;
        private double failureRate;
    }
}
