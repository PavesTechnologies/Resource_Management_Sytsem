package com.cdc.event;

import lombok.Data;

@Data
public class DebeziumPayload {

    private String op;        // c, u, d
    private PmsProjectAfter after;
}

