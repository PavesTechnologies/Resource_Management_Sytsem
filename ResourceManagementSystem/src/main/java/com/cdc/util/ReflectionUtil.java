package com.cdc.util;

import java.lang.reflect.Field;

public class ReflectionUtil {

    public static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to set field '" + fieldName + "' on " + target.getClass().getSimpleName(), e
            );
        }
    }

    private ReflectionUtil() {}
}
