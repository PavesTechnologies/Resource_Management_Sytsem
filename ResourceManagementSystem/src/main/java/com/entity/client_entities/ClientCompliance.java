package com.entity.client_entities;

import com.entity_enums.client_enums.RequirementType;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.Certificate;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "client_compliance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientCompliance {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "compliance_id")
    private UUID complianceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false, length = 20)
    private RequirementType requirementType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id")
    private Certificate certificate;

    @Column(nullable = false)
    private String requirementName;

    private Boolean mandatoryFlag;
    private Boolean activeFlag;
}

