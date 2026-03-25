package com.dto.roleoff_dto;

import com.entity_enums.roleoff_enums.RoleOffReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleOffExportDTO {
    
    private String resourceName;
    private String projectName;
    private String roleOffReason;
    private String effectiveRoleOffDate;
    private String roleInitiatedBy;
    private String projectId;
    private String clientId;
    private String roleOffStatus;
    
    // Date formatter for consistent export format
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");
    
    // Helper method to convert from RoleOffReportDTO to export format
    public static RoleOffExportDTO fromReportDTO(RoleOffReportDTO reportDTO) {
        if (reportDTO == null) return null;
        
        return RoleOffExportDTO.builder()
            .resourceName(reportDTO.getResourceName() != null ? reportDTO.getResourceName() : "")
            .projectName(reportDTO.getProjectName() != null ? reportDTO.getProjectName() : "")
            .roleOffReason(formatReasonForExport(reportDTO.getRoleOffReason()))
            .effectiveRoleOffDate(formatDateForExport(reportDTO.getEffectiveRoleOffDate()))
            .roleInitiatedBy(formatInitiatorForExport(reportDTO.getRoleInitiatedBy()))
            .projectId(reportDTO.getProjectId() != null ? reportDTO.getProjectId().toString() : "")
            .clientId(reportDTO.getClientId() != null ? reportDTO.getClientId().toString() : "")
            .roleOffStatus("")  // RoleOffReportDTO doesn't have roleOffStatus field
            .build();
    }
    
    // Helper method to format reason for export
    private static String formatReasonForExport(RoleOffReason reason) {
        if (reason == null) return "";
        switch (reason) {
            case PROJECT_END: return "Project End";
            case PERFORMANCE: return "Performance";
            case ATTRITION: return "Attrition";
            case CLIENT_REQUEST: return "Client Request";
            default: return reason.toString();
        }
    }
    
    // Helper method to format date for export
    private static String formatDateForExport(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMATTER);
    }
    
    // Helper method to format initiator for export
    private static String formatInitiatorForExport(String initiator) {
        if (initiator == null) return "";
        switch (initiator.toUpperCase()) {
            case "PROJECT-MANAGER": return "Project Manager";
            case "RESOURCE_MANAGER": return "Resource Manager";
            case "DELIVERY_LEAD": return "Delivery Lead";
            case "ADMIN": return "Admin";
            case "HR": return "HR";
            case "CLIENT_REQUEST": return "Client";
            default: return initiator;
        }
    }
    
    // Helper method to get CSV headers
    public static String[] getCsvHeaders() {
        return new String[]{
            "Resource Name",
            "Project Name", 
            "Role Off Reason",
            "Effective Date",
            "Initiated By",
            "Project ID",
            "Client ID"
        };
    }
    
    // Helper method to get CSV row data
    public String[] getCsvRow() {
        return new String[]{
            resourceName,
            projectName,
            roleOffReason,
            effectiveRoleOffDate,
            roleInitiatedBy,
            projectId,
            clientId
        };
    }
}
