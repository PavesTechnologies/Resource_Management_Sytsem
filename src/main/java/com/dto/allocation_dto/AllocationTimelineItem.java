package com.dto.allocation_dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationTimelineItem {
    
    private String project;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer allocation;
    private Boolean tentative;
}
