package com.entity.client_entities;

import com.entity_enums.centralised_enums.DeliveryModel;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RecordStatus;
import com.entity_enums.client_enums.ClientType;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@ToString
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clientId;

    @JsonProperty("client_name")
    @Column(nullable = false)
    private String clientName;

    @JsonProperty("client_type")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientType clientType;

//    @Column(nullable = false)
//    private String email;

    @JsonProperty("priority_level")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriorityLevel priorityLevel;

    @JsonProperty("delivery_model")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryModel deliveryModel;

//    @Column(nullable = false)
//    private String regionCode;

    @JsonProperty("country_name")
    @Column(nullable = false)
    private String countryName;

    @JsonProperty("default_timezone")
    @Column(nullable = false)
    private String defaultTimezone;

    @Enumerated(EnumType.STRING)
    private RecordStatus status;

    @JsonProperty("SLA")
    @Column(nullable = false)
    private Boolean sla;

    @Column(nullable = false)
    private Boolean compliance;

    @JsonProperty("escalationContact")
    @Column(nullable = false)
    private Boolean escalationContact;

    @Column(nullable = false)
    private Boolean assets;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
