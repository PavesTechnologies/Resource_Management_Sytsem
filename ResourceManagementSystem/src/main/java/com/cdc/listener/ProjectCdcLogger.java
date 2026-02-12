package com.cdc.listener;

import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;
import org.apache.kafka.connect.data.Struct;

@Component
public class ProjectCdcLogger {

    public void logEvent(RecordChangeEvent<SourceRecord> event) {

        SourceRecord record = event.record();
        Struct value = (Struct) record.value();

        if (value == null) {
            return;
        }

        String op = value.getString("op"); // c, u, d, r
        Struct source = value.getStruct("source");

        // CDC event processing logic here
    }
}
