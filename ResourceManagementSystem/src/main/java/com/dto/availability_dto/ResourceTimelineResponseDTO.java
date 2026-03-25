package com.dto.availability_dto;

import com.dto.skill_dto.SkillInfoDTO;
import com.dto.allocation_dto.AllocationTimelineItem;
import com.dto.allocation_dto.NoticeInfoDTO;
import com.dto.skill_dto.CertificationInfoDTO;
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
public class ResourceTimelineResponseDTO {
    
    private Long resourceId;
    private String name;
    private String avatar;
    private String role;
    private List<String> skills;
    private List<SkillInfoDTO> skillDetails;
    private List<CertificationInfoDTO> certifications;
    private String location;
    private Integer experience;
    private Integer currentAllocation;
    private LocalDate availableFrom;
    private List<String> currentProject;
    private NoticeInfoDTO noticeInfo;
    private String nextAssignment;
    private String employmentType;
    private List<AllocationTimelineItem> allocationTimeline;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineKPI {
        private Long totalResources;
        private Long fullyAvailable;
        private Long partiallyAvailable;
        private Long fullyAllocated;
        private Long overAllocated;
        private Long noticePeriodResources;
        private Long availableNoticePeriodResources;
    }
}
