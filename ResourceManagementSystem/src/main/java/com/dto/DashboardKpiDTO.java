package com.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpiDTO {
    
    private Long totalResources;
    private Long fullyAvailable;
    private Long partiallyAvailable;
    private Long fullyAllocated;
    private Long overAllocated;
    private Long upcomingAvailability;
    private Integer benchCapacity;
    private Integer utilization;
}
