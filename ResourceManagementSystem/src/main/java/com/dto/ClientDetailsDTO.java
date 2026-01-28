package com.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDetailsDTO {
    private String clientName;
    private String clientType;
    private String priorityLevel;
    private String deliveryModel;
    private String countryName;
    private String defaultTimezone;
    private String status;
//    private Projects projects;
}
