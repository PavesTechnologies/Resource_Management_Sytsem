package com.entity.allocation_entities;

import com.entity_enums.allocation_enums.AllocationModificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "allocation_modification_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AllocationModification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID modificationId;

    /**
     * Allocation to modify
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id", nullable = false)
    private ResourceAllocation allocation;

    /**
     * Current allocation %
     */
    private Integer currentAllocationPercentage;

    /**
     * Requested allocation %
     */
    private Integer requestedAllocationPercentage;

    /**
     * Effective date for modification
     */
    private LocalDate effectiveDate;

    /**
     * Reason for modification
     */
    private String reason;

    @Enumerated(EnumType.STRING)
    private AllocationModificationStatus status;

    private String requestedBy;

    @CreationTimestamp
    private LocalDateTime requestedAt;

    private String approvedBy;

    private LocalDateTime approvedAt;

    private String rejectReason;

    private String rejectedBy;

    @Column(name = "override_flag")
    private Boolean overrideFlag = false;

    @Column(name = "override_justification", length = 500)
    private String overrideJustification;

    @Column(name = "override_by")
    private String overrideBy;

    @Column(name = "override_at")
    private LocalDateTime overrideAt;
}
