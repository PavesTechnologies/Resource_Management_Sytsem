package com.dto.roleoff_dto;

import com.entity_enums.allocation_enums.RoleOffReason;
import com.entity_enums.allocation_enums.RoleOffStatus;
import com.entity_enums.demand_enums.ReplacementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleOffReportDTO {
    
    // ===== FILTER REQUEST FIELDS =====
    private LocalDate startDate;
    private LocalDate endDate;
    private List<UUID> clientIds;
    private List<Long> projectIds;
    private List<String> reasons;  // String to avoid enum casting issues
    
    // ===== ESSENTIAL INDIVIDUAL ROLE-OFF DATA =====
    private UUID id;
    private Long projectId;
    private String projectName;
    private UUID clientId;
    private Long resourceId;
    private String resourceName;
    private RoleOffReason roleOffReason;
    private LocalDate effectiveRoleOffDate;
    private String roleInitiatedBy;
    
    // ===== ESSENTIAL AGGREGATED REPORT DATA =====
    private Long totalRoleOffs;
    private List<RoleOffReportDTO> filteredEvents;
    private Map<String, Long> reasonBreakdown;
    
    // ===== RISK ANALYSIS DATA =====
    private List<RiskAlert> riskAlerts;
    private Map<String, Object> riskMetrics;
    private Boolean hasHighRiskPatterns;
    
    // ===== RISK ALERT INNER CLASS =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskAlert {
        private String type;
        private String severity;
        private String description;
        private Integer count;
        private List<String> affectedProjects;
        private List<String> affectedResources;
        private String recommendation;
    }
    
    // ===== HELPER METHODS =====
    
    // Helper method to convert string reasons to enum
    public List<RoleOffReason> getReasonsAsEnum() {
        if (reasons == null) return null;
        return reasons.stream()
            .map(reason -> {
                try {
                    return RoleOffReason.valueOf(reason);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            })
            .filter(reason -> reason != null)
            .collect(java.util.stream.Collectors.toList());
    }
    
    // Helper method to create event DTO from entity (essential fields only)
    public static RoleOffReportDTO fromEntity(com.entity.allocation_entities.RoleOffEvent event) {
        if (event == null) return null;
        
        return RoleOffReportDTO.builder()
            .id(event.getId())
            .projectId(event.getProject() != null ? event.getProject().getPmsProjectId() : null)
            .projectName(event.getProject() != null ? event.getProject().getName() : null)
            .clientId(event.getProject() != null ? event.getProject().getClientId() : null)
            .resourceId(event.getResource() != null ? event.getResource().getResourceId() : null)
            .resourceName(event.getResource() != null ? event.getResource().getFullName() : null)
            .roleOffReason(event.getRoleOffReasonEnum())
            .effectiveRoleOffDate(event.getEffectiveRoleOffDate())
            .roleInitiatedBy(event.getRoleInitiatedBy())
            .build();
    }
    
    // Helper method to create filter-only DTO
    public static RoleOffReportDTO createFilterRequest(LocalDate startDate, LocalDate endDate, 
                                                      List<UUID> clientIds, List<Long> projectIds, List<String> reasons) {
        return RoleOffReportDTO.builder()
            .startDate(startDate)
            .endDate(endDate)
            .clientIds(clientIds)
            .projectIds(projectIds)
            .reasons(reasons)
            .build();
    }
}
