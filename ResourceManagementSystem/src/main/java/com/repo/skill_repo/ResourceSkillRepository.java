package com.repo.skill_repo;

import com.entity.skill_entities.ResourceSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ResourceSkillRepository extends JpaRepository<ResourceSkill, UUID> {

    boolean existsByResourceIdAndSkillIdAndSubSkillIdAndActiveFlagTrue(
            UUID resourceId, UUID skillId, UUID subSkillId);

    boolean existsByResourceIdAndSkillIdAndActiveFlagTrue(
            UUID resourceId, UUID skillId);

    @Query("SELECT rs FROM ResourceSkill rs WHERE rs.resourceId = :resourceId AND rs.activeFlag = true")
    List<ResourceSkill> findByResourceIdAndActiveFlagTrue(@Param("resourceId") UUID resourceId);
}
