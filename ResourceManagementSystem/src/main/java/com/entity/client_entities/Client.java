package com.entity.client_entities;

import com.entity_enums.centralised_enums.DeliveryModel;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RecordStatus;
import com.entity_enums.client_enums.ClientType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "client")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clientId;

    @Column(nullable = false)
    private String clientName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientType clientType;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriorityLevel priorityLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryModel deliveryModel;

    @Column(nullable = false)
    private String regionCode;

    @Column(nullable = false)
    private String regionName;
    @Column(nullable = false)
    private String defaultTimezone;

    @Enumerated(EnumType.STRING)
    private RecordStatus status;

    @Column(nullable = false)
    private Boolean isSLA;

    @Column(nullable = false)
    private Boolean isCompliance;

    @Column(nullable = false)
    private Boolean isEscalationContact;

    @Column(nullable = false)
    private Boolean isAssets;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
