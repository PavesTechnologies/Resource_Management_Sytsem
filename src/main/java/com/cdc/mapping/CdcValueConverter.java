package com.cdc.mapping;

import com.cdc.parsing.SafeEnumParsingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ENHANCED CDC Value Converter with Safe Enum Parsing.
 * 
 * Enhanced with enterprise-grade enum parsing safety to handle
 * schema evolution and invalid enum values gracefully.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CdcValueConverter {

    private final SafeEnumParsingService safeEnumParsingService;

    /**
     * Convert CDC value with comprehensive error handling and safe enum parsing.
     * 
     * @param value Raw value from Debezium
     * @param type Target field type
     * @param enumClass Target enum class (for ENUM type)
     * @return Converted value with safety protections
     */
    public Object convert(Object value, FieldType type, Class<?> enumClass) {
        return convert(value, type, enumClass, "unknown-field");
    }

    /**
     * Convert CDC value with context for better error handling.
     * 
     * @param value Raw value from Debezium
     * @param type Target field type
     * @param enumClass Target enum class (for ENUM type)
     * @param context Context for logging (e.g., field name)
     * @return Converted value with safety protections
     */
    public Object convert(Object value, FieldType type, Class<?> enumClass, String context) {
        if (value == null) return null;

        try {
            return switch (type) {
                case STRING -> value.toString();
                case LONG -> convertToLong(value, context);
                case UUID -> convertToUUID(value, context);
                case BIG_DECIMAL -> convertToBigDecimal(value, context);
                case DOUBLE -> convertToDouble(value, context);
                case LOCAL_DATE -> convertToLocalDate(value, context);
                case LOCAL_DATE_TIME -> convertToLocalDateTime(value, context);
                case ENUM -> safeEnumParsingService.parseEnumSafe(value.toString(), (Class<? extends Enum>) enumClass, context);
            };
        } catch (Exception e) {
            // Log conversion error and return null or default
            log.error("Failed to convert value '{}' to type {} for context {}: {}", 
                     value, type, context, e.getMessage());
            return getDefaultValueForType(type, enumClass);
        }
    }

    /**
     * Safe Long conversion with error handling.
     */
    private Long convertToLong(Object value, String context) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.error("Invalid Long value '{}' for context {}: {}", value, context, e.getMessage());
            return 0L;
        }
    }

    /**
     * Safe UUID conversion with error handling.
     */
    private UUID convertToUUID(Object value, String context) {
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID value '{}' for context {}: {}", value, context, e.getMessage());
            return null;
        }
    }

    /**
     * Safe BigDecimal conversion with error handling.
     */
    private BigDecimal convertToBigDecimal(Object value, String context) {
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            log.error("Invalid BigDecimal value '{}' for context {}: {}", value, context, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Safe Double conversion with error handling.
     */
    private Double convertToDouble(Object value, String context) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            log.error("Invalid Double value '{}' for context {}: {}", value, context, e.getMessage());
            return 0.0;
        }
    }

    /**
     * Safe LocalDate conversion with enhanced error handling.
     */
    private LocalDate convertToLocalDate(Object value, String context) {
        String dateStr = value.toString();
        try {
            // Try parsing as ISO date first
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            try {
                // Try parsing as SQL date format
                if (dateStr.length() == 10) { // YYYY-MM-DD format
                    return LocalDate.parse(dateStr);
                } else {
                    // Try parsing as Unix timestamp (days)
                    long days = Long.parseLong(dateStr);
                    return LocalDate.ofEpochDay(days);
                }
            } catch (Exception e2) {
                log.error("Invalid LocalDate value '{}' for context {}: {}", value, context, e2.getMessage());
                return LocalDate.now();
            }
        }
    }

    /**
     * Safe LocalDateTime conversion with enhanced error handling.
     * Handles all Debezium MySQL CDC numeric forms:
     *   Integer → epoch days (DATE column)
     *   Long    → epoch micros / millis / seconds distinguished by magnitude
     */
    private LocalDateTime convertToLocalDateTime(Object value, String context) {
        // MySQL DATE column arrives as Integer (epoch days since 1970-01-01)
        if (value instanceof Integer) {
            return LocalDate.ofEpochDay((Integer) value).atStartOfDay();
        }
        String timestampStr = value.toString();
        try {
            return LocalDateTime.parse(timestampStr);
        } catch (Exception e) {
            try {
                long v = Long.parseLong(timestampStr);
                if (v > 1_000_000_000_000_000L) {        // microseconds (MySQL TIMESTAMP)
                    return LocalDateTime.ofEpochSecond(v / 1_000_000,
                            (int) ((v % 1_000_000) * 1000), java.time.ZoneOffset.UTC);
                } else if (v > 1_000_000_000_000L) {     // milliseconds (MySQL DATETIME)
                    return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(v), java.time.ZoneOffset.UTC);
                } else if (v < 100_000) {                 // epoch days (MySQL DATE as Long)
                    return LocalDate.ofEpochDay(v).atStartOfDay();
                } else {                                  // epoch seconds
                    return LocalDateTime.ofEpochSecond(v, 0, java.time.ZoneOffset.UTC);
                }
            } catch (Exception e2) {
                log.error("Invalid LocalDateTime value '{}' for context {}: {}", value, context, e2.getMessage());
                return LocalDateTime.now();
            }
        }
    }

    /**
     * Get default value for type when conversion fails.
     */
    private Object getDefaultValueForType(FieldType type, Class<?> enumClass) {
        return switch (type) {
            case STRING -> "";
            case LONG -> 0L;
            case UUID -> null;
            case BIG_DECIMAL -> BigDecimal.ZERO;
            case DOUBLE -> 0.0;
            case LOCAL_DATE -> LocalDate.now();
            case LOCAL_DATE_TIME -> LocalDateTime.now();
            case ENUM -> {
                if (enumClass != null && enumClass.isEnum()) {
                    Object[] constants = enumClass.getEnumConstants();
                    yield constants.length > 0 ? constants[0] : null;
                }
                yield null;
            }
        };
    }
}
