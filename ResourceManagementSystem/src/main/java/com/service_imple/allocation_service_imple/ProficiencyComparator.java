package com.service_imple.allocation_service_imple;

import com.entity.skill_entities.ProficiencyLevel;

/**
 * Utility class for proficiency comparison in skill gap matching
 * Provides deterministic comparison logic based on displayOrder
 */
public class ProficiencyComparator {
    
    /**
     * Compares resource proficiency against required proficiency
     * 
     * @param resourceProficiency Resource's current proficiency level
     * @param requiredProficiency Demand's required proficiency level
     * @return MATCH status string and score (1.0, 0.5, 0.0)
     */
    public static ProficiencyResult compareProficiency(ProficiencyLevel resourceProficiency, 
                                                   ProficiencyLevel requiredProficiency) {
        
        if (resourceProficiency == null || requiredProficiency == null) {
            return new ProficiencyResult("GAP", 0.0);
        }
        
        // Use displayOrder for numeric comparison if available
        if (resourceProficiency.getDisplayOrder() != null && requiredProficiency.getDisplayOrder() != null) {
            if (resourceProficiency.getDisplayOrder() >= requiredProficiency.getDisplayOrder()) {
                return new ProficiencyResult("MATCH", 1.0);
            } else {
                return new ProficiencyResult("PARTIAL", 0.5);
            }
        }
        
        // Fallback to string comparison if displayOrder not available
        if (resourceProficiency.getProficiencyCode().equals(requiredProficiency.getProficiencyCode())) {
            return new ProficiencyResult("MATCH", 1.0);
        } else {
            return new ProficiencyResult("PARTIAL", 0.5);
        }
    }
    
    /**
     * Result container for proficiency comparison
     */
    public static class ProficiencyResult {
        private final String status;
        private final Double score;
        
        public ProficiencyResult(String status, Double score) {
            this.status = status;
            this.score = score;
        }
        
        public String getStatus() { return status; }
        public Double getScore() { return score; }
    }
}
