package com.entity.skill_entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "skill_central_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillCentralAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "entity_name", nullable = false)
    private String entityName;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "operation", nullable = false)
    private String operation;
    // CREATE / UPDATE / DELETE

    @Column(name = "old_value", columnDefinition = "json")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "json")
    private String newValue;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}
