package com.entity.project_entities;

import com.entity.client_entities.ClientSLA;
import com.entity_enums.client_enums.SLAType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "project_sla")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectSLA {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "project_sla_id")
    private UUID projectSlaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "pms_project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_sla_id", referencedColumnName = "sla_id")
    private ClientSLA clientSLA;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)")
    private SLAType slaType;

    private Integer slaDurationDays;
    private Integer warningThresholdDays;
    private Boolean isInherited;
    private Boolean activeFlag;
}
