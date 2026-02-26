package com.entity.demand_entities;

import com.entity.project_entities.ProjectSLA;
import com.entity_enums.client_enums.SLAType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "demand_sla")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandSLA {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "demand_sla_id")
    private UUID demandSlaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_id", nullable = false)
    private Demand demand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_sla_id", nullable = false)
    private ProjectSLA projectSLA;

    // Freeze values from ProjectSLA
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)")
    private SLAType slaType;

    private Integer slaDurationDays;
    private Integer warningThresholdDays;

    private LocalDate createdAt;
    private LocalDate dueAt;

    private Boolean activeFlag;
}
