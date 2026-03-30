package com.entity.ledger_entities;

import com.entity_enums.ledger_enums.DLQStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dead_letter_queue",
       indexes = {
           @Index(name = "idx_event_id", columnList = "event_id"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_retry_count", columnList = "retry_count"),
           @Index(name = "idx_created_at", columnList = "created_at"),
           @Index(name = "idx_next_retry_at", columnList = "next_retry_at")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "max_retry_count", nullable = false)
    private Integer maxRetryCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DLQStatus status;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Column(name = "original_event_type", length = 50)
    private String originalEventType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
