package com.entity.ledger_entities;

import com.entity_enums.ledger_enums.EventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_event_log",
       indexes = {
           @Index(name = "idx_event_id", columnList = "event_id"),
           @Index(name = "idx_resource_id", columnList = "resource_id"),
           @Index(name = "idx_event_type", columnList = "event_type"),
           @Index(name = "idx_event_hash", columnList = "event_hash"),
           @Index(name = "idx_processed_flag", columnList = "processed_flag"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_next_retry_at", columnList = "next_retry_at"),
           @Index(name = "idx_connector_name", columnList = "connector_name"),
           @Index(name = "idx_entity_type", columnList = "entity_type"),
           @Index(name = "idx_entity_id", columnList = "entity_id"),
           @Index(name = "idx_claim_owner", columnList = "claim_owner"),
           @Index(name = "idx_created_at", columnList = "created_at")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "resource_id", nullable = false, length = 20)
    private String resourceId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_hash", nullable = false, length = 64)
    private String eventHash;

    @Column(name = "connector_name", length = 50)
    private String connectorName;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "source_table", length = 100)
    private String sourceTable;

    @Column(name = "operation_type", length = 20)
    private String operationType;

    @Column(name = "event_source", length = 50)
    private String eventSource;

    @Column(name = "payload", columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "source_timestamp")
    private Instant sourceTimestamp;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claim_owner", length = 120)
    private String claimOwner;

    @Column(name = "replay_of_event_id", length = 100)
    private String replayOfEventId;

    @Column(name = "last_error_at")
    private Instant lastErrorAt;

    @Column(name = "processed_flag", nullable = false)
    private Boolean processedFlag;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "processing_completed_at")
    private LocalDateTime processingCompletedAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
