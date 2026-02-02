package com.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "project")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    // 🔑 Primary Key (numeric, sourced from PMS)
    @Id
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "client_id")
    private Long clientId;

    // 📌 Status & Lifecycle
    @Column(name = "project_status")
    private String projectStatus;

    @Column(name = "lifecycle_stage")
    private String lifecycleStage;

    // 📅 Timeline
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // 🏢 Delivery model & location
    @Column(name = "delivery_model")
    private String deliveryModel;

    @Column(name = "primary_location")
    private String primaryLocation;

    // 👥 Ownership
    @Column(name = "project_manager_id")
    private Long projectManagerId;

    @Column(name = "resource_manager_id")
    private Long resourceManagerId;

    @Column(name = "delivery_lead_id")
    private Long deliveryLeadId;

    // ⚠️ Risk & priority
    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "risk_level_updated_at")
    private LocalDateTime riskLevelUpdatedAt;

    @Column(name = "priority_level")
    private String priorityLevel;

    // 🔄 CDC metadata
    @Column(name = "source_system")
    private String sourceSystem;

    @Column(name = "external_ref_id", unique = true)
    private String externalRefId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
}

