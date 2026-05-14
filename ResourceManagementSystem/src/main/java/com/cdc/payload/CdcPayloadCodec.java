package com.cdc.payload;

import com.cdc.util.CdcUtcSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.engine.RecordChangeEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CdcPayloadCodec {

    private static final TypeReference<CdcEventPayload> PAYLOAD_TYPE = new TypeReference<>() {};
    private final ObjectMapper objectMapper;
    private final CdcUtcSupport cdcUtcSupport;

    public CdcEventPayload fromEvent(String connectorName,
                                     String tableName,
                                     String operation,
                                     String entityType,
                                     String entityId,
                                     RecordChangeEvent<SourceRecord> event) {
        Struct value = (Struct) event.record().value();
        Map<String, Object> source = toMap(value != null ? value.getStruct("source") : null);
        Map<String, Object> before = toMap(value != null ? value.getStruct("before") : null);
        Map<String, Object> after = toMap(value != null ? value.getStruct("after") : null);
        Instant sourceTimestamp = cdcUtcSupport.extractSourceTimestamp(source, after != null ? after : before);

        return CdcEventPayload.builder()
                .connectorName(connectorName)
                .tableName(tableName)
                .operation(operation)
                .entityType(entityType)
                .entityId(entityId)
                .sourceTimestamp(sourceTimestamp)
                .source(source)
                .before(before)
                .after(after)
                .build();
    }

    public String serialize(CdcEventPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize CDC payload", e);
        }
    }

    public CdcEventPayload deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, PAYLOAD_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to deserialize CDC payload", e);
        }
    }

    public String computeEventHash(CdcEventPayload payload) {
        return sha256(serialize(payload));
    }

    public String computeEventId(String connectorName, RecordChangeEvent<SourceRecord> event, CdcEventPayload payload) {
        SourceRecord record = event.record();
        return sha256(connectorName
                + "|" + record.sourcePartition()
                + "|" + record.sourceOffset()
                + "|" + payload.getEntityType()
                + "|" + payload.getEntityId()
                + "|" + payload.getOperation());
    }

    private Map<String, Object> toMap(Struct struct) {
        if (struct == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (Field field : struct.schema().fields()) {
            Object value = struct.get(field);
            map.put(field.name(), value instanceof Struct nested ? toMap(nested) : value);
        }
        return map;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute CDC hash", e);
        }
    }
}
