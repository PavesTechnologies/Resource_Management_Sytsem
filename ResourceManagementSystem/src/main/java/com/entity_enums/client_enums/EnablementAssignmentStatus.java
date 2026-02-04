package com.entity_enums.client_enums;

public enum EnablementAssignmentStatus {
    REQUESTED,   // Resource Manager requested enablement
    ASSIGNED,    // Client / Admin assigned it
    IN_USE,      // Asset is currently being used
    REJECTED,    // Client rejected the request
    RETURNED,    // Asset has been returned
    LOST         // Asset is lost
}
