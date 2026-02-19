package com.repo.skill_repo;

import com.entity.skill_entities.DemandRoleSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemandRoleSkillRepo extends JpaRepository<DemandRoleSkill, Long> {
}

