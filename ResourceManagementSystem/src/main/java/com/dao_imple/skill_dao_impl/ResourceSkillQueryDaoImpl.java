package com.dao_imple.skill_dao_impl;

import com.dao_interface.skill_dao.ResourceSkillQueryDao;
import com.entity.skill_entities.ResourceSkill;
import com.repo.skill_repo.ResourceSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ResourceSkillQueryDaoImpl implements ResourceSkillQueryDao {

    private final ResourceSkillRepository resourceSkillRepo;

    @Override
    public Optional<ResourceSkill> findActiveSkillForResource(Long resourceId, Long skillId) {
        return resourceSkillRepo.findByResourceIdAndSkillId(resourceId, skillId)
                .filter(ResourceSkill::getActiveFlag);
    }

    @Override
    public List<ResourceSkill> findAllActiveSkillsForResource(Long resourceId) {
        return resourceSkillRepo.findByResourceIdAndActiveFlagTrue(resourceId);
    }
}
