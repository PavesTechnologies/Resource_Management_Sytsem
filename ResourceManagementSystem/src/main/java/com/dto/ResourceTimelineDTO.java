package com.dto;

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
public class ResourceTimelineDTO {
    
    private String id;
    private String name;
    private String avatar;
    private String role;
    private List<String> skills;
    private String location;
    private Integer experience;
    private Integer currentAllocation;
    private LocalDate availableFrom;
    private List<String> currentProject;
    private String nextAssignment;
    private String employmentType;
    private List<Integer> utilizationHistory;
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
}
