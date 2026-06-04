package com.cdc.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdcEventPayload {

    private String connectorName;
    private String tableName;
    private String operation;
    private String entityType;
    private String entityId;
    private Instant sourceTimestamp;
    private Map<String, Object> source;
    private Map<String, Object> before;
    private Map<String, Object> after;
}
