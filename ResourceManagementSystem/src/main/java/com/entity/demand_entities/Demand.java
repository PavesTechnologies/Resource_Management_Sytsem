package com.entity.demand_entities;

import com.entity.project_entities.Project;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.demand_enums.DemandStatus;
import com.entity_enums.demand_enums.DemandType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "demand")
@Data
public class Demand {

    @Id
    @Column(name = "demand_id", unique = true)
    private UUID demandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false, referencedColumnName = "pms_project_id")
    @JsonIgnore
    private Project project;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "role_id")
    private UUID roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "demand_type", nullable = false, length = 20)
    private DemandType demandType;

    @Column(name = "outgoing_resource_id")
    private Long outgoingResourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "demand_status", nullable = false, length = 20)
    private DemandStatus demandStatus;

    @Column(name = "requires_additional_approval")
    private Boolean requiresAdditionalApproval;

    @Enumerated(EnumType.STRING)
    @Column(name = "demand_priority", length = 20)
    private PriorityLevel demandPriority;

    @Column(name = "demand_start_date")
    private LocalDate demandStartDate;

    @Column(name = "demand_end_date")
    private LocalDate demandEndDate;

    @Column(name = "allocation_percentage")
    private Integer allocationPercentage;

    @Column(name = "location_requirement", length = 255)
    private String locationRequirement;

    @Column(name = "delivery_model", length = 100)
    private String deliveryModel;

    @Column(name = "demand_justification", length = 1000)
    private String demandJustification;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        if (demandId == null) {
            demandId = UUID.randomUUID();
        }
        if (demandStatus == null) {
            demandStatus = DemandStatus.DRAFT;
        }
        if (requiresAdditionalApproval == null) {
            requiresAdditionalApproval = false;
        }
    }
}