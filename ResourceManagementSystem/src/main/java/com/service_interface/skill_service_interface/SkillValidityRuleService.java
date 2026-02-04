package com.service_interface.skill_service_interface;

import com.entity.skill_entities.SkillValidityRule;
import com.dto.UserDTO;

import java.util.Optional;
import java.util.UUID;

public interface SkillValidityRuleService {

    SkillValidityRule defineRule(
            UUID skillId,
            Integer validityPeriodMonths,
            Integer recencyThresholdMonths,
            Boolean expiryRequired,
            UserDTO user
    );

    Optional<SkillValidityRule> getRule(UUID skillId);
}
