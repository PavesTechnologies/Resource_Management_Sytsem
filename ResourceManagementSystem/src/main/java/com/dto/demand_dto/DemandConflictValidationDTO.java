package com.dto.demand_dto;

import lombok.Data;
import java.util.List;

@Data
public class DemandConflictValidationDTO {
    private boolean hasConflicts;
    private boolean canSubmit;
    private List<ConflictDetail> conflicts;
    private String validationMessage;
    
    @Data
    public static class ConflictDetail {
        private String conflictType;
        private String severity; // ERROR, WARNING, INFO
        private String description;
        private String impact;
        private String suggestedResolution;
        private boolean requiresAction;
        
        public static ConflictDetail error(String type, String description, String impact, String resolution) {
            ConflictDetail detail = new ConflictDetail();
            detail.conflictType = type;
            detail.severity = "ERROR";
            detail.description = description;
            detail.impact = impact;
            detail.suggestedResolution = resolution;
            detail.requiresAction = true;
            return detail;
        }
        
        public static ConflictDetail warning(String type, String description, String impact, String resolution) {
            ConflictDetail detail = new ConflictDetail();
            detail.conflictType = type;
            detail.severity = "WARNING";
            detail.description = description;
            detail.impact = impact;
            detail.suggestedResolution = resolution;
            detail.requiresAction = false;
            return detail;
        }
    }
}
