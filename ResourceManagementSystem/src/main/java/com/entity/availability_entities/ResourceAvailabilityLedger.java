package com.entity.availability_entities;

import com.entity.resource_entities.Resource;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_availability_ledger", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "period_start"}),
       indexes = {
           @Index(name = "idx_resource_period", columnList = "resource_id, period_start"),
           @Index(name = "idx_period_range", columnList = "period_start, period_end"),
           @Index(name = "idx_trust_flag", columnList = "availability_trust_flag")
       })
@Data
@Builder
public class ResourceAvailabilityLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
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
