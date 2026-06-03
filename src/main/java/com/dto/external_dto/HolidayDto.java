package com.dto.external_dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HolidayDto {
    private Integer holidayId;
    private String holidayName;
    private LocalDate holidayDate;
    private String type;
    private String state;
    private String country;
    private Integer year;
    private Boolean isActive;
}
