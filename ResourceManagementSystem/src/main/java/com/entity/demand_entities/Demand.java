package com.entity.demand_entities;

import com.entity.project_entities.Project;
import com.entity_enums.demand_enums.DemandType;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.skill_enums.DemandStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "demand")
@Data
public class Demand {

    @Id
    @UuidGenerator
    @Column(name = "demand_id")
    private UUID demandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "role_id", nullable = false)
//    private DeliveryRole role;

    @Column(name = "demand_justification", length = 500)
    private String demandJustification;

    @Column(name = "demand_start_date", nullable = false)
    private LocalDate demandStartDate;

    @Column(name = "demand_end_date")
    private LocalDate demandEndDate;

    @Column(name = "allocation_percentage")
    private Integer allocationPercentage;

    @Column(name = "location_requirement", length = 100)
    private String locationRequirement;

    @Column(name = "delivery_model", length = 50)
    private String deliveryModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "demand_type_name", nullable = false, length = 20)
    private DemandType demandType;

    @Enumerated(EnumType.STRING)
    @Column(name = "demand_status", length = 30)
    private DemandStatus demandStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "demand_priority", nullable = false, length = 20)
    private PriorityLevel demandPriority;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
