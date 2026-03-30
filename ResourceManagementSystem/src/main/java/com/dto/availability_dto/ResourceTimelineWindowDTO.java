package com.dto.availability_dto;

import com.dto.allocation_dto.AllocationTimelineItem;
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
    
    private ResourceTimelineResponseDTO.ResourceTimelineKpi timelineKpi;
}
