package com.entity.allocation_entities;

import com.entity.demand_entities.Demand;
import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity_enums.allocation_enums.RoleOffReason;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "resource_allocation",
        indexes = {
                @Index(
                        name = "idx_resource_allocation_dates",
                        columnList = "resource_id, allocation_start_date, allocation_end_date"
                ),
                @Index(
                        name = "idx_allocation_status",
                        columnList = "allocation_status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "fieldHandler"})
public class ResourceAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "allocation_id")
    private UUID allocationId;

    /**
     * Resource being allocated
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false, referencedColumnName = "resource_id")
    private Resource resource;

    /**
     * Demand-based allocation (nullable)
     * Exactly one of demandId or projectId must be present
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_id", referencedColumnName = "demand_id")
    private Demand demand;

    /**
     * Project-based allocation (nullable)
     * Exactly one of demandId or projectId must be present
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "pms_project_id")
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

    @Column(name = "override_flag")
    private Boolean overrideFlag = false;

    @Column(name = "override_justification", length = 500)
    private String overrideJustification;

    @Column(name = "override_by")
    private String overrideBy;

    @Column(name = "override_at")
    private LocalDateTime overrideAt;

    @Column(name = "role_off_date")
    private LocalDate roleOffDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_off_reason")
    private RoleOffReason roleOffReason;
}
