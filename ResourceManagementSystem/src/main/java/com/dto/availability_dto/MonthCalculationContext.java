package com.dto.availability_dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
public class MonthCalculationContext {
    private Long resourceId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate dateOfJoining;
    private LocalDate dateOfExit;
    private Set<LocalDate> holidays;
    private Set<LocalDate> leaveDates;
    private boolean apisHealthy;
    private boolean isActiveResource;
}
