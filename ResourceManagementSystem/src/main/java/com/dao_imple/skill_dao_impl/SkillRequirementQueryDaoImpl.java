package com.dao_imple.skill_dao_impl;

import com.dao_interface.skill_dao.SkillRequirementQueryDao;
import com.entity.skill_entities.SkillRequirement;
import com.entity_enums.skill_enums.AppliesToType;
import com.repo.skill_repo.SkillRequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SkillRequirementQueryDaoImpl implements SkillRequirementQueryDao {

    private final SkillRequirementRepository requirementRepo;

    @Override
    public List<SkillRequirement> findRequirements(
            AppliesToType appliesToType,
            Long appliesToId
    ) {
        return requirementRepo.findByAppliesToTypeAndAppliesToId(appliesToType, appliesToId);
    }
}
