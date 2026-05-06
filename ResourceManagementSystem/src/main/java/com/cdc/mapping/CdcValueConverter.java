package com.cdc.mapping;

import com.cdc.parsing.SafeEnumParsingService;
import lombok.RequiredArgsConstructor;
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
            System.err.println("Failed to convert value '" + value + "' to type " + type + 
                             " for context " + context + ": " + e.getMessage());
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
            System.err.println("Invalid Long value '" + value + "' for context " + context + ": " + e.getMessage());
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
            System.err.println("Invalid UUID value '" + value + "' for context " + context + ": " + e.getMessage());
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
            System.err.println("Invalid BigDecimal value '" + value + "' for context " + context + ": " + e.getMessage());
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
            System.err.println("Invalid Double value '" + value + "' for context " + context + ": " + e.getMessage());
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
                System.err.println("Invalid LocalDate value '" + value + "' for context " + context + ": " + e2.getMessage());
                return LocalDate.now();
            }
        }
    }

    /**
     * Safe LocalDateTime conversion with enhanced error handling.
     */
    private LocalDateTime convertToLocalDateTime(Object value, String context) {
        String timestampStr = value.toString();
        try {
            // Try parsing as formatted date string first
            return LocalDateTime.parse(timestampStr);
        } catch (Exception e) {
            try {
                // Try parsing as Unix timestamp in microseconds
                long micros = Long.parseLong(timestampStr);
                return LocalDateTime.ofEpochSecond(
                    micros / 1_000_000, 
                    (int) ((micros % 1_000_000) * 1000), 
                    java.time.ZoneOffset.UTC
                );
            } catch (Exception e2) {
                System.err.println("Invalid LocalDateTime value '" + value + "' for context " + context + ": " + e2.getMessage());
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
