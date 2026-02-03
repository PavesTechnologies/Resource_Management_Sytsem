package com.repo.skill_repo;

import com.entity.skill_entities.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

    Optional<Skill> findBySkillNameIgnoreCase(String skillName);

    boolean existsByCategoryCategoryIdAndSkillNameIgnoreCase(UUID categoryId, String skillName);

    List<Skill> findByCategoryCategoryIdAndActiveFlagTrue(UUID categoryId);
}
