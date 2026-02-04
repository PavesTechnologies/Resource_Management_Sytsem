package com.cdc.util;

import com.cdc.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CdcValueConverter {

    public static Object convert(Object value, FieldType type, Class<?> enumClass) {
        if (value == null) return null;

        return switch (type) {
            case STRING -> value.toString();
            case LONG -> ((Number) value).longValue();
            case UUID -> UUID.fromString(value.toString());
            case BIG_DECIMAL -> new BigDecimal(value.toString());
            case LOCAL_DATE_TIME -> {
                String timestampStr = value.toString();
                try {
                    // Try parsing as formatted date string first
                    yield LocalDateTime.parse(timestampStr);
                } catch (Exception e) {
                    // If that fails, treat as Unix timestamp in microseconds
                    long micros = Long.parseLong(timestampStr);
                    yield LocalDateTime.ofEpochSecond(
                        micros / 1_000_000, 
                        (int) ((micros % 1_000_000) * 1000), 
                        java.time.ZoneOffset.UTC
                    );
                }
            }
            case ENUM -> Enum.valueOf((Class<Enum>) enumClass, value.toString());
        };
    }
}
