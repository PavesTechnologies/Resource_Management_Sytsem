package com.entity_enums.ledger_enums;

public enum DLQStatus {
    PENDING_RETRY,
    FAILED,
    RETRY_EXHAUSTED,
    MANUALLY_PROCESSED,
    PERMANENTLY_FAILED
}
