package com.entity.demand_entities;

import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.Certificate;
import com.entity_enums.centralised_enums.DeliveryModel;
import com.entity_enums.demand_enums.DemandCommitment;
import com.entity_enums.demand_enums.DemandStatus;
import com.entity_enums.demand_enums.DemandType;
import com.entity_enums.centralised_enums.PriorityLevel;
//import com.entity_enums.skill_enums.DemandStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

@Entity
@Table(name = "demand")
@Data
public class Demand {

    @Id
    @UuidGenerator
    @Column(name = "demand_id")
    private UUID demandId;

    @Column(name = "demand_name", nullable = false)
    private String demandName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "pms_project_id",nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private DeliveryRoleExpectation role;

    @Column(name = "demand_justification", length = 500)
    private String demandJustification;

    @Column(name = "demand_start_date", nullable = false)
    private LocalDate demandStartDate;

    @Column(name = "demand_end_date",nullable = false)
    private LocalDateTime demandEndDate;

    @Column(name = "allocation_percentage",nullable = false)
    private Integer allocationPercentage;

//    @Column(name = "location_requirement", length = 100)
//    private String locationRequirement;

    @Column(name = "delivery_model", length = 50)
    private DeliveryModel deliveryModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "demand_type_name", nullable = false, length = 20)
    private DemandType demandType;

    @Enumerated(EnumType.STRING)
    @Column(name = "demand_status", length = 30)
    private DemandStatus demandStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "demand_priority", nullable = false, length = 20)
    private PriorityLevel demandPriority;

    @Column(name = "min_exp", nullable = false)
    private Integer minExp;

    @Column(name = "resources_required", nullable = false)
    private Integer resourcesRequired;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outgoing_resource_id")
    private Resource outgoingResource;

    @Column(name = "requires_additional_approval")
    private Boolean requiresAdditionalApproval = false;

    @ManyToMany
    @JoinTable(
        name = "demand_skill",
        joinColumns = @JoinColumn(name = "demand_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Set<Skill> requiredSkills = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "demand_certificate",
        joinColumns = @JoinColumn(name = "demand_id"),
        inverseJoinColumns = @JoinColumn(name = "certificate_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Set<Certificate> requiredCertificates = new HashSet<>();

    @Column(name = "demand_commitment", length = 500,nullable = false)
    private DemandCommitment demandCommitment;

    @Column(name = "soft_demand_expiry")
    private LocalDate softDemandExpiry;
}
