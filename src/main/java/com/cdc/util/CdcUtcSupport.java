package com.cdc.util;

import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Map;

@Component
public class CdcUtcSupport {

    public Instant now() {
        return Instant.now();
    }

    public LocalDateTime utcDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public Instant extractSourceTimestamp(Map<String, Object> source, Map<String, Object> after) {
        Instant fromSource = extractInstant(source != null ? source.get("ts_ms") : null);
        if (fromSource != null) {
            return fromSource;
        }

        if (after != null) {
            for (String field : new String[]{"updated_at", "modified_at", "last_updated", "change_timestamp", "event_time", "created_at"}) {
                Instant candidate = extractInstant(after.get(field));
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        return null;
    }

    public Instant extractInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof Number number) {
            long raw = number.longValue();
            if (raw > 1_000_000_000_000_000L) {
                long seconds = raw / 1_000_000L;
                long nanos = (raw % 1_000_000L) * 1_000L;
                return Instant.ofEpochSecond(seconds, nanos);
            }
            if (raw > 1_000_000_000_000L) {
                return Instant.ofEpochMilli(raw);
            }
            return Instant.ofEpochSecond(raw);
        }
        if (value instanceof String stringValue) {
            String trimmed = stringValue.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return Instant.parse(trimmed);
            } catch (DateTimeParseException ignored) {
            }
            try {
                return LocalDateTime.parse(trimmed).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
            }
            try {
                return LocalDate.parse(trimmed).atStartOfDay().toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
            }
            try {
                return extractInstant(Long.parseLong(trimmed));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
