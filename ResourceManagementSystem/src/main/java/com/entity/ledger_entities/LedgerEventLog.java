package com.entity.ledger_entities;

import com.entity_enums.ledger_enums.EventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_hash", nullable = false, length = 64)
    private String eventHash;

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
