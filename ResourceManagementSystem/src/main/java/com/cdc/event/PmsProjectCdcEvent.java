package com.cdc.event;

import lombok.Data;

@Data
public class PmsProjectCdcEvent {

    private DebeziumPayload payload;
}

