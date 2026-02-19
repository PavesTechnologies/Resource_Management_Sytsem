package com.repo.skill_repo;

import com.entity.skill_entities.RoleTemplateSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleTemplateSkillRepo extends JpaRepository<RoleTemplateSkill, Long> {

    List<RoleTemplateSkill> findByRoleTemplate_RoleTemplateId(Long roleTemplateId);
}

