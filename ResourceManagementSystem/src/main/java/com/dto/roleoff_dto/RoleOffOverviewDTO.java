package com.dto.roleoff_dto;


import com.entity_enums.roleoff_enums.RoleOffReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleOffOverviewDTO {
    
    // Basic counts
    private Long totalRoleOffs;
    private Long thisMonthRoleOffs;
    private Long lastMonthRoleOffs;
    
    // Reason breakdown
    private Map<RoleOffReason, Long> reasonCounts;
    
    // Simple trend
    private String trend; // "UP", "DOWN", "STABLE"
    
    // Top risk projects
    private java.util.List<ProjectRiskDTO> topRiskProjects;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectRiskDTO {
        private String projectName;
        private Long roleOffCount;
        private String mainReason;
    }
}
