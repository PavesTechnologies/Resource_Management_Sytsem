package com.dto.roleoff_dto;

import com.entity_enums.allocation_enums.AllocationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResourcesDTO {
    private Long resourceId;
    private String name;
    private String department;
    private String projectName;
    private String clientName;
    private String demandName;
    private List<String> skills;
    private List<String> subSkills;
    private AllocationStatus status;
    private Integer allocationPercentage;
    private LocalDate endDate;
}
