package com.entity.project_entities;

import com.entity.client_entities.ClientCompliance;
import com.entity_enums.client_enums.RequirementType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "project_compliance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProjectCompliance {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "project_compliance_id")
    private UUID projectComplianceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, referencedColumnName = "pms_project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_compliance_id", referencedColumnName = "compliance_id")
    private ClientCompliance clientCompliance;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)")
    private RequirementType requirementType;

    private String requirementName;
    private Boolean mandatoryFlag;
    private Boolean isInherited;
    private Boolean activeFlag;
}
