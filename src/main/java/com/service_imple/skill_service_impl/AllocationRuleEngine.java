package com.service_imple.skill_service_impl;

import com.dto.allocation_dto.AllocationRequestDTO;
import com.dto.skill_dto.AllocationValidationRequestDTO;
import com.service_interface.skill_service_interface.ProficiencyValidationService;
import com.service_interface.skill_service_interface.SkillGovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AllocationRuleEngine {
    private final SkillGovernanceService skillGovernanceService;
    private final ProficiencyValidationService proficiencyValidationService;
    private final CertificationComplianceServiceImpl certificationComplianceService;

    public void validateAllocation(AllocationValidationRequestDTO dto) {
        dto.getSkillRequirements().forEach(req -> {

            // 1️⃣ Validate skill framework
            skillGovernanceService
                    .validateSkillFrameworkReadiness(req.getSkillId());

            // 2️⃣ Validate proficiency
            proficiencyValidationService
                    .validateProficiency(
                            dto.getResourceId(),
                            req.getSkillId(),
                            req.getRequiredProficiencyId());

            // 3️⃣ Validate certification
            certificationComplianceService
                    .validateCertificationCompliance(dto);
        });
    }
}
