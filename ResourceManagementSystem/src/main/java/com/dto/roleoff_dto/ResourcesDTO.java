package com.dto.roleoff_dto;

import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SubSkill;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.allocation_enums.RoleOffStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResourcesDTO {
    private UUID roleOffId;
    private Long resourceId;
    private String name;
    private String department;
    private String projectName;
    private String clientName;
    private UUID deliveryRoleId;
    private String demandName;
    private List<String> skills;
    private List<String> subSkills;
    private UUID allocationId;
    private String impact;
    private AllocationStatus status;
    private RoleOffStatus roleOffStatus;
    private Integer allocationPercentage;
    private LocalDate endDate;
    private LocalDate effectiveDate;
    private String rejectedBy;
    private String rejectionReason;
}
