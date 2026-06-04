package com.entity_enums.allocation_enums;

/**
 * Allocation Type - distinguishes between immediate and future allocations
 * ACTIVE: Resource allocation starts immediately from current date
 * PLANNED: Resource allocation is planned for a future start date
 */
public enum AllocationType {
    ACTIVE,
    PLANNED
}
