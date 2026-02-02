package com.cdc.util;

import org.apache.kafka.connect.data.Struct;

import java.util.HashSet;
import java.util.Set;

public class DebeziumChangeDetector {

    public static Set<String> detectChangedColumns(Struct before, Struct after) {
        Set<String> changed = new HashSet<>();

        if (before == null) {
            after.schema().fields().forEach(f -> changed.add(f.name()));
            return changed;
        }

        after.schema().fields().forEach(field -> {
            Object beforeVal = before.get(field);
            Object afterVal = after.get(field);

            if (beforeVal == null && afterVal != null ||
                    beforeVal != null && !beforeVal.equals(afterVal)) {
                changed.add(field.name());
            }
        });

        return changed;
    }
}
