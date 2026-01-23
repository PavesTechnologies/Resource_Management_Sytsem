package com.entity.client_entities;

import com.entity_enums.client_enums.SLAType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "client_sla")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientSLA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long slaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    private SLAType slaType;

    private Integer slaDurationDays;
    private Integer warningThresholdDays;
    private Boolean activeFlag;
}

