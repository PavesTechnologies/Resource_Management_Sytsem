package com.cdc.listener;

import io.debezium.engine.RecordChangeEvent;

import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.apache.kafka.connect.data.Struct;




@Component
public class ProjectCdcLogger {

    private static final Logger log =
            LoggerFactory.getLogger(ProjectCdcLogger.class);

    public void logEvent(RecordChangeEvent<SourceRecord> event) {

        SourceRecord record = event.record();
        Struct value = (Struct) record.value();

        if (value == null) {
            return;
        }

        String op = value.getString("op"); // c, u, d, r
        Struct source = value.getStruct("source");

        log.info("CDC EVENT RECEIVED | op={} | table={}",
                op,
                source.getString("table"));
    }
}

