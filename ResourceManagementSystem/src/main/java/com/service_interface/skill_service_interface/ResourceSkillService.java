package com.service_interface.skill_service_interface;

import com.entity.skill_entities.ResourceSkill;
import com.dto.UserDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ResourceSkillService {

    ResourceSkill addOrUpdateResourceSkill(
            Long resourceId,
            UUID skillId,
            LocalDate lastUsedDate,
            LocalDate expiryDate,
            UserDTO user
    );

    List<ResourceSkill> getActiveSkillsForResource(Long resourceId);
}
