package com.dao_imple.skill_dao_impl;

import com.dao_interface.skill_dao.SkillTaxonomyQueryDao;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SkillCategory;
import com.entity.skill_entities.SubSkill;
import com.repo.skill_repo.SkillCategoryRepository;
import com.repo.skill_repo.SkillRepository;
import com.repo.skill_repo.SubSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SkillTaxonomyQueryDaoImpl implements SkillTaxonomyQueryDao {

    private final SkillCategoryRepository categoryRepo;
    private final SkillRepository skillRepo;
    private final SubSkillRepository subSkillRepo;

    @Override
    public Optional<SkillCategory> findActiveCategoryByName(String categoryName) {
        return categoryRepo.findByCategoryNameIgnoreCase(categoryName)
                .filter(SkillCategory::getActiveFlag);
    }

    @Override
    public Optional<Skill> findActiveSkillByNameAndCategory(String skillName, Long categoryId) {
        return skillRepo.findBySkillNameIgnoreCase(skillName)
                .filter(skill -> skill.getCategory().getCategoryId().equals(categoryId))
                .filter(Skill::getActiveFlag);
    }

    @Override
    public List<Skill> findActiveSkillsByCategory(Long categoryId) {
        return skillRepo.findByCategoryCategoryIdAndActiveFlagTrue(categoryId);
    }

    @Override
    public List<SubSkill> findActiveSubSkillsBySkill(Long skillId) {
        return subSkillRepo.findBySkillSkillIdAndActiveFlagTrue(skillId);
    }
}
