package com.cdc.failure;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cdc_failure")
@Data
public class CdcFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // Example: PROJECT
    @Column(name = "entity_type", nullable = false)
    private String entityType;

    // PMS primary key
    @Column(name = "entity_id", nullable = false)
    private String entityId;

    // INSERT / UPDATE / DELETE
    @Column(name = "operation", nullable = false)
    private String operation;

    // Root cause classification
    @Column(name = "error_type", nullable = false)
    private String errorType;

    // Human readable error
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Serialized payload (JSON string)
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    // Retry metadata
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "status", nullable = false)
    private String status; // NEW, RETRYING, RESOLVED, DEAD

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
