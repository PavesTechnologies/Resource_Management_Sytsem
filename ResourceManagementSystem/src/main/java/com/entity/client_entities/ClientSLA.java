package com.entity.client_entities;

import com.entity_enums.client_enums.SLAType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "client_sla")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientSLA {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "sla_id")
    private UUID slaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)")
    private SLAType slaType;

    private Integer slaDurationDays;
    private Integer warningThresholdDays;
    private Boolean activeFlag;
}

