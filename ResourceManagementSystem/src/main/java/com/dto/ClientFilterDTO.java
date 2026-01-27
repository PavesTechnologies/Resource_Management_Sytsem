package com.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientFilterDTO {

    private String clientName;
    private String clientType;
    private String priorityLevel;
    private String deliveryModel;
    private String regionCode;
    private String regionName;
    private String defaultTimezone;
    private String status;

    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
    private LocalDateTime updatedFrom;
    private LocalDateTime updatedTo;
}

