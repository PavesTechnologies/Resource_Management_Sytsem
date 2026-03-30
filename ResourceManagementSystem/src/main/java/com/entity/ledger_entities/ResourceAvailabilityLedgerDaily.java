package com.entity.ledger_entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_availability_ledger_daily", 
       indexes = {
           @Index(name = "idx_resource_id", columnList = "resource_id"),
           @Index(name = "idx_date", columnList = "date"),
           @Index(name = "idx_resource_date", columnList = "resource_id, date"),
           @Index(name = "idx_trust_flag", columnList = "availability_trust_flag"),
           @Index(name = "idx_last_event", columnList = "last_event_id"),
           @Index(name = "idx_updated_at", columnList = "updated_at")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_resource_date", columnNames = {"resource_id", "date"})
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceAvailabilityLedgerDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

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

    @Column(name = "total_allocation_percentage", nullable = false)
    private Integer totalAllocationPercentage;

    @Column(name = "available_percentage", nullable = false)
    private Integer availablePercentage;

    @Column(name = "is_overallocated", nullable = false)
    private Boolean isOverallocated;

    @Column(name = "over_allocation_percentage", nullable = false)
    private Integer overAllocationPercentage;

    @Column(name = "availability_trust_flag", nullable = false)
    private Boolean availabilityTrustFlag;

    @Column(name = "calculation_version", nullable = false)
    private Long calculationVersion;

    @Column(name = "last_event_id")
    private String lastEventId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
