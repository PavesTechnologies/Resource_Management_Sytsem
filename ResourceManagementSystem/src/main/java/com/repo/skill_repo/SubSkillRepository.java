package com.repo.skill_repo;

import com.entity.skill_entities.SubSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface SubSkillRepository extends JpaRepository<SubSkill, UUID> {

    boolean existsByNameIgnoreCaseAndSkill_Id(String name, UUID skillId);

    boolean existsByNameIgnoreCaseAndSkill_IdAndIdNot(String name, UUID skillId, UUID id);

    List<SubSkill> findByStatusIgnoreCase(String status);

    List<SubSkill> findBySkill_IdAndStatusIgnoreCase(UUID skillId, String status);

    @Query("SELECT ss FROM SubSkill ss WHERE ss.status = 'ACTIVE' ORDER BY ss.name")
    List<SubSkill> findActiveSubSkills();

    @Modifying
    @Transactional
    @Query("UPDATE SubSkill ss SET ss.status = 'INACTIVE' WHERE ss.id = :subSkillId")
    int deactivateSubSkill(@Param("subSkillId") UUID subSkillId);

    @Query("SELECT ss FROM SubSkill ss JOIN FETCH ss.skill WHERE ss.skill.id = :skillId AND ss.status = 'ACTIVE' ORDER BY ss.name")
    List<SubSkill> findActiveSubSkillsBySkillId(@Param("skillId") UUID skillId);

    @Query("SELECT ss FROM SubSkill ss JOIN FETCH ss.skill WHERE ss.skill.id IN :skillIds AND ss.status = 'ACTIVE' ORDER BY ss.skill.name, ss.name")
    List<SubSkill> findActiveSubSkillsBySkillIds(@Param("skillIds") List<UUID> skillIds);
}

