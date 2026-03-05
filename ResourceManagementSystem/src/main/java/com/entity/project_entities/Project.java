package com.entity.project_entities;

import com.entity.client_entities.Client;
import com.entity_enums.project_enums.ProjectDataStatus;
import com.entity_enums.project_enums.ProjectStage;
import com.entity_enums.project_enums.ProjectStatus;
import com.entity_enums.centralised_enums.DeliveryModel;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RiskLevel;
import com.entity_enums.project_enums.StaffingReadinessStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project")
@Data
public class Project {


    @Id
    @Column(name = "pms_project_id", unique = true)
    private Long pmsProjectId;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", insertable = false, updatable = false, referencedColumnName = "client_id")
    @JsonIgnore
    private Client client;

    // Client reference (cross-system safe)
    @Column(name = "client_id")
    private UUID clientId;

    // Business owner / sponsor
    @Column(name = "project_manager_id")
    private Long projectManagerId;

    // Resource Manager
    @Column(name = "resource_manager_id")
    private Long resourceManagerId;

    // Delivery Owner
    @Column(name = "delivery_owner_id")
    private Long deliveryOwnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_model", length = 20)
    private DeliveryModel deliveryModel;

    @Column(name = "primary_location", length = 100)
    private String primaryLocation;

    /* =====================
       RISK & PRIORITY (USED BY RMS)
       ===================== */

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private RiskLevel riskLevel;

    @Column(name = "risk_level_updated_at")
    private LocalDateTime riskLevelUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", length = 20)
    private PriorityLevel priorityLevel;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;


    /* =====================
       BUDGET (SYNCED FROM PMS)
       ===================== */

    // Approved budget snapshot (read-only)
    @Column(name = "project_budget", precision = 15, scale = 2)
    private BigDecimal projectBudget;

    @Column(name = "project_budget_currency", length = 3)
    private String projectBudgetCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_status", length = 20)
    private ProjectStatus projectStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_stage", length = 30)
    private ProjectStage lifecycleStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_status", length = 20)
    private ProjectDataStatus dataStatus;

    @Column(name = "last_synced_at")
    private LocalDateTime LastSyncedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "staffing_readiness_status", length = 20)
    private StaffingReadinessStatus staffingReadinessStatus;

    @Column(name = "staffing_readiness_reason", length = 255)
    private String staffingReadinessReason;

    @Column(name = "staffing_readiness_updated_at")
    private LocalDateTime staffingReadinessUpdatedAt;

    /* =====================
       AUDIT
       ===================== */

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private boolean hasOverlap;

    public boolean getHasOverlap() {
        return hasOverlap;
    }

    public void setHasOverlap(boolean hasOverlap) {
        this.hasOverlap = hasOverlap;
    }

    public Long getId() {
        return this.pmsProjectId;
    }

}
