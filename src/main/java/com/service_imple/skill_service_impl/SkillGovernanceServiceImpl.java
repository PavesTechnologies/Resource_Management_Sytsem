package com.service_imple.skill_service_impl;

import com.entity.skill_entities.Skill;
import com.global_exception_handler.SkillExceptionHandler;
import com.repo.skill_repo.SkillRepository;
import com.service_interface.skill_service_interface.SkillGovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SkillGovernanceServiceImpl implements SkillGovernanceService {
    private final SkillRepository skillRepository;

    @Override
    public void validateSkillFrameworkReadiness(UUID skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() ->
                        SkillExceptionHandler.badRequest("Skill not found"));

        if (!"ACTIVE".equals(skill.getStatus())) {
            throw SkillExceptionHandler.badRequest(
                    "Skill is inactive");
        }

        if (skill.getCategory() == null) {
            throw SkillExceptionHandler.badRequest(
                    "Skill taxonomy incomplete");
        }
    }
}
