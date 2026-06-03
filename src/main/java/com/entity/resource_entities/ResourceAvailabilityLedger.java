package com.entity.resource_entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_availability_ledger", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "period_start"}))
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourceAvailabilityLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false, referencedColumnName = "resource_id")
    private Resource resource;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "standard_hours", nullable = false)
    private Integer standardHours;

    @Column(name = "holiday_hours", nullable = false)
    private Integer holidayHours;

    @Column(name = "leave_hours", nullable = false)
    private Integer leaveHours;

    @Column(name = "confirmed_alloc_hours", nullable = false)
    private Integer confirmedAllocHours;

    @Column(name = "draft_alloc_hours", nullable = false)
    private Integer draftAllocHours;

    @Column(name = "total_allocation", nullable = false)
    private Integer totalAllocation;

    @Column(name = "available_percentage", nullable = false)
    private Integer availablePercentage;

    @Column(name = "firm_available_hours", nullable = false)
    private Integer firmAvailableHours;

    @Column(name = "projected_available_hours", nullable = false)
    private Integer projectedAvailableHours;

    @Column(name = "availability_trust_flag", nullable = false)
    private Boolean availabilityTrustFlag;

    @Column(name = "last_calculated_at", nullable = false)
    private LocalDateTime lastCalculatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;
}
