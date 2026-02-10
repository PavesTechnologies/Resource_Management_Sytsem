package com.dto.external;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

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
