package com.dto.client_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
