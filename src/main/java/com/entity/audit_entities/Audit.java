package com.entity.audit_entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "fieldHandler"})
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "module", nullable = false, length = 50)
    private String module;

    @Column(name = "entity", nullable = false, length = 100)
    private String entity;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "user", length = 255)
    private String user;

    @Column(name = "transaction_id", length = 36)
    private String transactionId;

    @Column(name = "request_id", length = 36)
    private String requestId;

    @Column(name = "source", length = 20)
    private String source;

    @Column(name = "before_state", columnDefinition = "TEXT")
    private String before;

    @Column(name = "after_state", columnDefinition = "TEXT")
    private String after;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false, nullable = false)
    private LocalDateTime timestamp;
}
