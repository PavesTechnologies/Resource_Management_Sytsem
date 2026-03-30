package com.entity_enums.allocation_enums;

public enum AllocationStatus {
    PLANNED,     // Tentative (affects projected availability)
    ACTIVE,      // Confirmed (affects firm availability)
    ENDED,       // Historical, ignored in calculations
    CANCELLED,
    FULFILLED// Ignored in calculations
}
