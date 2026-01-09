package com.service_interface.skill_service_interface;

import com.entity.skill_entities.ResourceSkill;
import com.dto.UserDTO;

import java.time.LocalDate;
import java.util.List;

public interface ResourceSkillService {

    ResourceSkill addOrUpdateResourceSkill(
            Long resourceId,
            Long skillId,
            LocalDate lastUsedDate,
            LocalDate expiryDate,
            UserDTO user
    );

    List<ResourceSkill> getActiveSkillsForResource(Long resourceId);
}
