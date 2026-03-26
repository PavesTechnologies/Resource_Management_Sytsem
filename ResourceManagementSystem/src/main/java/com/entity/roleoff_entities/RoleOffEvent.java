package com.entity.roleoff_entities;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity_enums.roleoff_enums.RoleOffStatus;
import com.entity_enums.roleoff_enums.RoleOffType;
import com.entity_enums.demand_enums.ReplacementStatus;
import com.entity_enums.roleoff_enums.RoleOffReason;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "role_off_event")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "fieldHandler"})
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

@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = true)
    private DeliveryRoleExpectation role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id", nullable = false)
    private ResourceAllocation allocation;

    @Enumerated(EnumType.STRING)
    private RoleOffType roleOffType;

    private LocalDate effectiveRoleOffDate;

//    private LocalDate projectEndDate;

    @Column(name = "role_off_reason_text")
    private String roleOffReason;

    @Enumerated(EnumType.STRING)
    private RoleOffStatus roleOffStatus;

    @Enumerated(EnumType.STRING)
    private ReplacementStatus replacementStatus;

    private String skipReason;

    // Autofill values on create/update
    @Enumerated(EnumType.STRING)
    @Column(name = "role_off_reason")
    private RoleOffReason roleOffReasonEnum;

    @CreationTimestamp
    private LocalDate createdAt;
    @UpdateTimestamp
    private LocalDate updatedAt;
    private Long createdBy;
    @Column(name = "rm_approved")
    private Boolean rmApproved;
    // NEW FIELDS
    private Boolean dlApproved;
    private LocalDate dlActionDate;
    private String rejectedBy;
    private String rejectionReason;
    private String roleInitiatedBy;
}
