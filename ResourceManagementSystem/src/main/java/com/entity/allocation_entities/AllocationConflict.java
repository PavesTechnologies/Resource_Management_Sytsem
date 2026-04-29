package com.entity.allocation_entities;

import com.audit.AuditEntityListener;
import com.entity.resource_entities.Resource;
import com.entity_enums.allocation_enums.AllocationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "allocation_conflicts")
@EntityListeners(AuditEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationConflict {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "conflict_id")
    private UUID conflictId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(name = "lower_priority_allocation_id")
    private UUID lowerPriorityAllocationId;

    @Column(name = "higher_priority_allocation_id")
    private UUID higherPriorityAllocationId;

    @Column(name = "lower_priority_client_name")
    private String lowerPriorityClientName;

    @Column(name = "higher_priority_client_name")
    private String higherPriorityClientName;

    @Column(name = "lower_priority_client_tier")
    private String lowerPriorityClientTier;

    @Column(name = "higher_priority_client_tier")
    private String higherPriorityClientTier;

    @Column(name = "lower_priority_allocation_type")
    private String lowerPriorityAllocationType;

    @Column(name = "higher_priority_allocation_type")
    private String higherPriorityAllocationType;

    @Column(name = "conflict_type", nullable = false)
    private String conflictType; // "PRIORITY_MISMATCH"

    @Column(name = "resolution_status")
    private String resolutionStatus; // "PENDING", "RESOLVED", "IGNORED"

    @Column(name = "resolution_action")
    private String resolutionAction; // "UPGRADE", "DISPLACE", "KEEP_CURRENT"

    @Column(name = "conflict_severity")
    private String conflictSeverity; // "MEDIUM", "HIGH"

    @Column(name = "recommendation", length = 500)
    private String recommendation;

    @CreationTimestamp
    @Column(name = "detected_at", updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;
}
