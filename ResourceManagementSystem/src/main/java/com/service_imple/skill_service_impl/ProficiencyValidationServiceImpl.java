package com.service_imple.skill_service_impl;

import com.entity.skill_entities.ResourceSkill;
import com.global_exception_handler.GovernanceViolationException;
import com.repo.skill_repo.ResourceSkillRepository;
import com.service_interface.skill_service_interface.ProficiencyValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProficiencyValidationServiceImpl implements ProficiencyValidationService {
    private final ResourceSkillRepository resourceSkillRepository;

    @Override
    public void validateProficiency(Long resourceId, UUID skillId, UUID requiredProficiencyId) {
        ResourceSkill rs =
                resourceSkillRepository
                        .findByResourceIdAndSkillIdAndActiveFlagTrue(
                                resourceId, skillId)
                        .orElseThrow(() ->
                                new GovernanceViolationException(
                                        "Skill missing for resource"));

        if (!rs.getProficiencyId().equals(requiredProficiencyId)) {
            throw new GovernanceViolationException(
                    "Proficiency mismatch");
        }
    }
}
