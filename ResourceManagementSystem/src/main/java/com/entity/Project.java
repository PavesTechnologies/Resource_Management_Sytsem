package com.entity;

import com.entity_enums.ProjectStage;
import com.entity_enums.ProjectStatus;
import com.entity_enums.centralised_enums.DeliveryModel;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RiskLevel;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(name = "last_synced_at")
    private LocalDateTime LastSyncedAt;

    /* =====================
       AUDIT
       ===================== */

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
