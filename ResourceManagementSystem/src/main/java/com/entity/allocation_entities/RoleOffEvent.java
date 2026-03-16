package com.entity.allocation_entities;

import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity_enums.allocation_enums.RoleOffStatus;
import com.entity_enums.allocation_enums.RoleOffType;
import com.entity_enums.demand_enums.ReplacementStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "role_off_event")
@Data
public class RoleOffEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "role_id")
//    private DeliveryRoleExpectation role;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id", nullable = false)
    private ResourceAllocation allocation;

    @Enumerated(EnumType.STRING)
    private RoleOffType roleOffType;

    private LocalDate effectiveRoleOffDate;

//    private LocalDate projectEndDate;

    private String roleOffReason;

    @Enumerated(EnumType.STRING)
    private RoleOffStatus roleOffStatus;

    @Enumerated(EnumType.STRING)
    private ReplacementStatus replacementStatus;

    private String skipReason;

    // Autofill values on create/update
    @CreationTimestamp
    private LocalDate createdAt;
    @UpdateTimestamp
    private LocalDate updatedAt;
    private Long createdBy;
    private String approvedBy;
    private String rejectedBy;
    private String rejectionReason;
    private String roleInitiatedBy;
}
