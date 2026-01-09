package com.repo.skill_repo;

import com.entity.skill_entities.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findBySkillNameIgnoreCase(String skillName);

    boolean existsByCategoryCategoryIdAndSkillNameIgnoreCase(Long categoryId, String skillName);

    List<Skill> findByCategoryCategoryIdAndActiveFlagTrue(Long categoryId);
}
