package com.entity.client_entities;

import com.audit.AuditEntityListener;
import com.entity_enums.centralised_enums.DeliveryModel;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RecordStatus;
import com.entity_enums.client_enums.ClientType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "client")
@EntityListeners(AuditEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "client_id")
    private UUID clientId;

    @JsonProperty("client_name")
    @Column(nullable = false)
    @NotBlank(message = "Client name is required")
    @Size(min = 3, max = 100, message = "Client name must be between 3 and 100 characters")
    @Pattern(
            regexp = "^[A-Za-z]+(?:[ .][A-Za-z]+)*$",
            message = "Client name can contain only letters, spaces, and dots"
    )
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
