package com.cdc.mapping;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ColumnMapping {
    private String pmsColumn;   // column name in PMS (Debezium struct)
    private String rmsField;    // field name in RMS entity
    private FieldType fieldType;
    private Class<?> enumClass; // only for ENUM, else null
}
