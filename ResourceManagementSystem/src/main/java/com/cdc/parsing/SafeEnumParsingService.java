package com.cdc.parsing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ENTERPRISE-GRADE Safe Enum Parsing Service.
 * 
 * CRITICAL SAFETY MECHANISM to handle enum parsing failures gracefully
 * during CDC processing and schema evolution.
 * 
 * Features:
 * - Safe enum conversion with fallback handling
 * - Enum value mapping for renamed/deprecated values
 * - Default value assignment for invalid enums
 * - Enum drift detection and logging
 * - Case-insensitive enum parsing
 */
@Slf4j
@Service
public class SafeEnumParsingService {

    // Enum value mappings for renamed/deprecated values
    private final Map<String, Map<String, String>> enumMappings = new ConcurrentHashMap<>();
    
    // Default values for enum types
    private final Map<String, String> defaultEnumValues = new ConcurrentHashMap<>();
    
    // Invalid enum tracking for monitoring
    private final Map<String, Map<String, Integer>> invalidEnumCounts = new ConcurrentHashMap<>();

    /**
     * Safe enum parsing with comprehensive error handling.
     * 
     * @param value Raw enum value to parse
     * @param enumClass Target enum class
     * @param context Context for logging (e.g., field name, table name)
     * @return Parsed enum value or default fallback
     */
    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> T parseEnumSafe(String value, Class<T> enumClass, String context) {
        if (value == null || enumClass == null) {
            return getDefaultValue(enumClass, context);
        }

        try {
            // Check for mapped enum values (renamed/deprecated)
            String mappedValue = getMappedEnumValue(enumClass.getSimpleName(), value);
            String finalValue = mappedValue != null ? mappedValue : value;
            
            // Try case-insensitive parsing first
            T enumValue = parseEnumCaseInsensitive(finalValue, enumClass);
            if (enumValue != null) {
                return enumValue;
            }
            
            // Fall back to standard enum parsing
            return Enum.valueOf(enumClass, finalValue.toUpperCase());
            
        } catch (IllegalArgumentException e) {
            // Enum parsing failed - handle gracefully
            return handleInvalidEnum(value, enumClass, context, e);
        } catch (Exception e) {
            // Unexpected error - log and return default
            log.error("Unexpected error parsing enum value '{}' for {} in context {}: {}", 
                     value, enumClass.getSimpleName(), context, e.getMessage(), e);
            return getDefaultValue(enumClass, context);
        }
    }

    /**
     * Register enum value mapping for renamed/deprecated values.
     * 
     * @param enumTypeName Enum type name (simple class name)
     * @param oldValue Old/deprecated enum value
     * @param newValue New/current enum value
     */
    public void registerEnumMapping(String enumTypeName, String oldValue, String newValue) {
        enumMappings.computeIfAbsent(enumTypeName, k -> new ConcurrentHashMap<>()).put(oldValue, newValue);
        log.info("Registered enum mapping for {}: {} -> {}", enumTypeName, oldValue, newValue);
    }

    /**
     * Register default value for enum type.
     * 
     * @param enumTypeName Enum type name
     * @param defaultValue Default enum value
     */
    public void registerDefaultValue(String enumTypeName, String defaultValue) {
        defaultEnumValues.put(enumTypeName, defaultValue);
        log.info("Registered default value for {}: {}", enumTypeName, defaultValue);
    }

    /**
     * Get enum parsing statistics.
     * 
     * @return Enum parsing statistics
     */
    public EnumParsingStatistics getStatistics() {
        return EnumParsingStatistics.builder()
                .trackedEnumTypes(enumMappings.size())
                .totalMappings(enumMappings.values().stream().mapToInt(Map::size).sum())
                .totalDefaults(defaultEnumValues.size())
                .invalidEnumInstances(invalidEnumCounts.values().stream().mapToInt(m -> m.values().stream().mapToInt(Integer::intValue).sum()).sum())
                .build();
    }

    /**
     * Reset invalid enum counters.
     * 
     * @param enumTypeName Optional enum type to reset (null for all)
     */
    public void resetInvalidEnumCounters(String enumTypeName) {
        if (enumTypeName == null) {
            invalidEnumCounts.clear();
            log.info("Reset all invalid enum counters");
        } else {
            invalidEnumCounts.remove(enumTypeName);
            log.info("Reset invalid enum counters for {}", enumTypeName);
        }
    }

    /**
     * Parse enum with case-insensitive matching.
     */
    @SuppressWarnings("unchecked")
    private <T extends Enum<T>> T parseEnumCaseInsensitive(String value, Class<T> enumClass) {
        try {
            // Try exact match first (case-sensitive)
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            // Try case-insensitive matching
            String upperValue = value.toUpperCase();
            for (T enumConstant : enumClass.getEnumConstants()) {
                if (enumConstant.name().equalsIgnoreCase(upperValue)) {
                    return enumConstant;
                }
            }
            return null;
        }
    }

    /**
     * Handle invalid enum value gracefully.
     */
    @SuppressWarnings("unchecked")
    private <T extends Enum<T>> T handleInvalidEnum(String value, Class<T> enumClass, String context, Exception originalException) {
        String enumTypeName = enumClass.getSimpleName();
        
        // Track invalid enum for monitoring
        invalidEnumCounts.computeIfAbsent(enumTypeName, k -> new ConcurrentHashMap<>())
                        .merge(value, 1, Integer::sum);
        
        // Log warning with context
        log.warn("Invalid enum value '{}' for {} in context {}: {}. Using default value. Original error: {}", 
                value, enumTypeName, context, originalException.getMessage(), originalException.getMessage());
        
        // Check if we have too many invalid values (potential schema drift)
        Map<String, Integer> typeInvalidCounts = invalidEnumCounts.get(enumTypeName);
        long totalInvalid = typeInvalidCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (totalInvalid > 100) { // Threshold for alerting
            log.error("High number of invalid enum values detected for {}: {} total invalid values. " +
                     "Possible schema drift or data corruption. Invalid values: {}", 
                     enumTypeName, totalInvalid, typeInvalidCounts);
        }
        
        return getDefaultValue(enumClass, context);
    }

    /**
     * Get default value for enum type.
     */
    @SuppressWarnings("unchecked")
    private <T extends Enum<T>> T getDefaultValue(Class<T> enumClass, String context) {
        String enumTypeName = enumClass.getSimpleName();
        String defaultValueName = defaultEnumValues.get(enumTypeName);
        
        if (defaultValueName != null) {
            try {
                return Enum.valueOf(enumClass, defaultValueName);
            } catch (IllegalArgumentException e) {
                log.warn("Default enum value '{}' not found for {} in context {}. Using first enum constant.", 
                        defaultValueName, enumTypeName, context);
            }
        }
        
        // Fall back to first enum constant
        T[] enumConstants = enumClass.getEnumConstants();
        if (enumConstants != null && enumConstants.length > 0) {
            log.debug("Using first enum constant '{}' as default for {} in context {}", 
                    enumConstants[0].name(), enumTypeName, context);
            return enumConstants[0];
        }
        
        log.error("No enum constants available for {} in context {}. Returning null.", enumTypeName, context);
        return null;
    }

    /**
     * Get mapped enum value for renamed/deprecated values.
     */
    private String getMappedEnumValue(String enumTypeName, String value) {
        Map<String, String> mappings = enumMappings.get(enumTypeName);
        return mappings != null ? mappings.get(value) : null;
    }

    /**
     * Enum parsing statistics.
     */
    @lombok.Builder
    @lombok.Data
    public static class EnumParsingStatistics {
        private int trackedEnumTypes;
        private int totalMappings;
        private int totalDefaults;
        private int invalidEnumInstances;
    }

    /**
     * Initialize common enum mappings and defaults.
     * Called during service initialization.
     */
    public void initializeCommonMappings() {
        // Employment Status mappings
        registerEnumMapping("EmploymentStatus", "ACTIVE", "ACTIVE");
        registerEnumMapping("EmploymentStatus", "INACTIVE", "INACTIVE");
        registerEnumMapping("EmploymentStatus", "EXITED", "EXITED");
        registerEnumMapping("EmploymentStatus", "ON_LEAVE", "ON_LEAVE");
        registerDefaultValue("EmploymentStatus", "ACTIVE");
        
        // Project Status mappings
        registerEnumMapping("ProjectStatus", "ACTIVE", "ACTIVE");
        registerEnumMapping("ProjectStatus", "COMPLETED", "COMPLETED");
        registerEnumMapping("ProjectStatus", "CANCELLED", "CANCELLED");
        registerEnumMapping("ProjectStatus", "ON_HOLD", "ON_HOLD");
        registerDefaultValue("ProjectStatus", "ACTIVE");
        
        // Allocation Status mappings
        registerEnumMapping("AllocationStatus", "PLANNED", "PLANNED");
        registerEnumMapping("AllocationStatus", "APPROVED", "APPROVED");
        registerEnumMapping("AllocationStatus", "ACTIVE", "ACTIVE");
        registerEnumMapping("AllocationStatus", "CLOSED", "CLOSED");
        registerDefaultValue("AllocationStatus", "PLANNED");
        
        log.info("Initialized common enum mappings and defaults");
    }
}
