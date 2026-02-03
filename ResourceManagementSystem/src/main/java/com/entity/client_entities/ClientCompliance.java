package com.entity.client_entities;

import com.entity_enums.client_enums.ComplianceType;
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
    private UUID complianceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    private ComplianceType requirementType;

    private String requirementName;
    private Boolean mandatoryFlag;
    private Boolean activeFlag;
}

