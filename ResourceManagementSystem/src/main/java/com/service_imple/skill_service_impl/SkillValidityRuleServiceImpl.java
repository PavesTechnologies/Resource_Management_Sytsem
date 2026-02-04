package com.service_imple.skill_service_impl;

import com.dto.UserDTO;
import com.entity.skill_entities.SkillValidityRule;
import com.repo.skill_repo.SkillValidityRuleRepository;
import com.service_interface.skill_service_interface.SkillAuditService;
import com.service_interface.skill_service_interface.SkillValidityRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SkillValidityRuleServiceImpl implements SkillValidityRuleService {

    private final SkillValidityRuleRepository ruleRepo;
    private final SkillAuditService auditService;

    @Override
    public SkillValidityRule defineRule(UUID skillId,
                                        Integer validityPeriodMonths,
                                        Integer recencyThresholdMonths,
                                        Boolean expiryRequired,
                                        UserDTO user) {

        Optional<SkillValidityRule> existingOpt = ruleRepo.findById(skillId);

        SkillValidityRule rule = SkillValidityRule.builder()
                .skillId(skillId)
                .validityPeriodMonths(validityPeriodMonths)
                .recencyThresholdMonths(recencyThresholdMonths)
                .expiryRequired(expiryRequired)
                .build();

        SkillValidityRule saved = ruleRepo.save(rule);

        existingOpt.ifPresentOrElse(
                existing -> auditService.auditUpdate(
                        "SKILL_VALIDITY_RULE",
                        skillId.toString(),
                        existing,
                        saved,
                        user.getEmail()
                ),
                () -> auditService.auditCreate(
                        "SKILL_VALIDITY_RULE",
                        skillId.toString(),
                        saved,
                        user.getEmail()
                )
        );

        return saved;
    }

    @Override
    public Optional<SkillValidityRule> getRule(UUID skillId) {
        return ruleRepo.findById(skillId);
    }
}
