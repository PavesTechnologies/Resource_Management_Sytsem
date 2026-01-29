package com.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ClientFilterDTO {

    private String clientName;
    private String clientType;
    private String priorityLevel;
    private String deliveryModel;
//    private String regionCode;
    private String countryName;
    private String defaultTimezone;
    private String status;

    private LocalDate createdFrom;
    private LocalDate createdTo;
}

