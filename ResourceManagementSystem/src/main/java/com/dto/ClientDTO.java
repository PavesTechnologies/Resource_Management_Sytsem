package com.dto;

import com.entity_enums.centralised_enums.DeliveryModel;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RecordStatus;
import com.entity_enums.client_enums.ClientType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDTO {
    private Long clientId;
    private String clientName;
    private ClientType clientType;
    private String email;
    private PriorityLevel priorityLevel;
    private DeliveryModel deliveryModel;
    private String regionCode;
    private String regionName;
    private String defaultTimezone;
    private RecordStatus status;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
