package com.service_imple.skill_service_impl;

import com.entity.skill_entities.ResourceSkill;
import com.global_exception_handler.SkillExceptionHandler;
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
    public void validateProficiency(String resourceId, UUID skillId, UUID requiredProficiencyId) {
        ResourceSkill rs =
                resourceSkillRepository
                        .findByResourceIdAndSkillIdAndActiveFlagTrue(
                                resourceId, skillId)
                        .orElseThrow(() ->
                                SkillExceptionHandler.badRequest(
                                        "Skill missing for resource"));

        if (!rs.getProficiencyId().equals(requiredProficiencyId)) {
            throw SkillExceptionHandler.badRequest(
                    "Proficiency mismatch");
        }
    }
}
