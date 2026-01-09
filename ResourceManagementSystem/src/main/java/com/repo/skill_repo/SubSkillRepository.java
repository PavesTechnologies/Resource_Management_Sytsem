package com.repo.skill_repo;

import com.entity.skill_entities.SubSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubSkillRepository extends JpaRepository<SubSkill, Long> {

    List<SubSkill> findBySkillSkillIdAndActiveFlagTrue(Long skillId);
}
