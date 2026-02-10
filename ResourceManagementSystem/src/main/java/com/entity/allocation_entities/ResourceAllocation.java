package com.entity.allocation_entities;

import com.entity.Demand;
import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import com.entity_enums.allocation_enums.AllocationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resource_allocation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID allocationId;

    /**
     * Resource being allocated
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    /**
     * Demand-based allocation (nullable)
     * Exactly one of demandId or projectId must be present
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_id")
    private Demand demand;

    /**
     * Project-based allocation (nullable)
     * Exactly one of demandId or projectId must be present
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /**
     * Allocation start date (inclusive)
     */
    @Column(name = "allocation_start_date", nullable = false)
    private LocalDate allocationStartDate;

    /**
     * Allocation end date (inclusive)
     */
    @Column(name = "allocation_end_date", nullable = false)
    private LocalDate allocationEndDate;

    /**
     * Allocation percentage (1–100)
     */
    @Column(name = "allocation_percentage", nullable = false)
    private Integer allocationPercentage;

    /**
     * Planned / Active / Ended / Cancelled
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_status", nullable = false)
    private AllocationStatus allocationStatus;

    /**
     * User or system who created this allocation
     */
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    /**
     * Allocation creation timestamp
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
