package com.dto.bench_dto;

import com.entity_enums.bench.BenchReason;
import com.entity_enums.bench.SubState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for Bench Resource information to be returned to frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchResourceDTO {
    
    private Long resourceId;
    private String resourceName;
    private String email;
    private String designation;
    private String primarySkillGroup;
    private Long experience;
    private String workingLocation;
    private String employmentType;
    private String workforceCategory;
    
    // Bench specific fields
    private LocalDate benchStartDate;
    private BenchReason benchReason;
    private SubState subState;
    private Long benchDays;
    
    // Additional fields for frontend
    private String grade;
    private String vendorName;
    private LocalDate dateOfJoining;
    private BigDecimal hourlyCostRate;
    private String currencyType;
}
