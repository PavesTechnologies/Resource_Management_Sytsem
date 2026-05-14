package com.entity_enums.ledger_enums;

public enum EventStatus {
    NEW,
    CLAIMED,
    PROCESSING,
    SUCCESS,
    RETRY_SCHEDULED,
    DEAD_LETTER,
    CANCELLED,

    // Legacy statuses retained for backward compatibility with existing rows
    PENDING,
    FAILED,
    RETRY_EXHAUSTED,
    PERMANENTLY_FAILED
}
