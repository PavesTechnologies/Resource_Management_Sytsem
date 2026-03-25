package com.dto.availability_dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceTimelineWindowDTO {
    
    private Long id;
    private String name;
    private String avatar;
    private String role;
    private List<String> skills;
    private String location;
    private Integer experience;
    private String employmentType;
    private Double avgAllocation;
    private String status;
    private LocalDate availableFrom;
    private List<String> currentProjects;
    private String nextAssignment;
    private List<AllocationTimelineItem> allocationTimeline;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationTimelineItem {
        private String project;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer allocation;
        private Boolean tentative;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineKPI {
        private Long totalResources;
        private Long availableCount;
        private Long partialCount;
        private Long allocatedCount;
        private Double avgAllocationOverall;
        private Long overAllocatedCount;
    }
}
