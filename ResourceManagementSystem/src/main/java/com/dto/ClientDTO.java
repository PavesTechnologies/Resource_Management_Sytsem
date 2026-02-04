package com.dto;

import com.entity_enums.centralised_enums.DeliveryModel;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RecordStatus;
import com.entity_enums.client_enums.ClientType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDTO {

    private UUID clientId;
    private String clientName;
    private String clientType;
    private String priorityLevel;
    private String deliveryModel;
    //    private String regionCode;
    private String countryName;
    private String defaultTimezone;
    private String status;
}
