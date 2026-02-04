package com.service_imple.skill_service_impl;

import com.dto.UserDTO;
import com.entity.skill_entities.SkillRequirement;
import com.entity_enums.skill_enums.AppliesToType;
import com.repo.skill_repo.SkillRequirementRepository;
import com.service_interface.skill_service_interface.SkillAuditService;
import com.service_interface.skill_service_interface.SkillRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SkillRequirementServiceImpl implements SkillRequirementService {

    private final SkillRequirementRepository requirementRepo;
    private final SkillAuditService auditService;

    @Override
    public SkillRequirement addRequirement(AppliesToType appliesToType,
                                           Long appliesToId,
                                           UUID skillId,
                                           String proficiencyName,
                                           Boolean mandatory,
                                           UserDTO user) {

        SkillRequirement req = SkillRequirement.builder()
                .appliesToType(appliesToType)
                .appliesToId(appliesToId)
                .skillId(skillId)
                .proficiencyName(proficiencyName)
                .mandatoryFlag(mandatory)
                .build();

        SkillRequirement saved = requirementRepo.save(req);

        auditService.auditCreate(
                "SKILL_REQUIREMENT",
                saved.getRequirementId().toString(),
                saved,
                user.getEmail()
        );

        return saved;
    }

    @Override
    public List<SkillRequirement> getRequirements(AppliesToType appliesToType, Long appliesToId) {
        return requirementRepo.findByAppliesToTypeAndAppliesToId(appliesToType, appliesToId);
    }
}
