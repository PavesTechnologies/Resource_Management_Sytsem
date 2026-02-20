package com.entity.client_entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "client_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "audit_id")
    private UUID auditId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "client_id")
    private Client client;

    private String entityName;
    private String fieldName;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    private String changedBy;
    private LocalDateTime changedAt;
}

