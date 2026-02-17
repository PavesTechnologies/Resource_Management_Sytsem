package com.repo.skill_repo;

import com.entity.skill_entities.ResourceSubSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ResourceSubSkillRepository extends JpaRepository<ResourceSubSkill, UUID> {

    boolean existsByResourceIdAndSubSkillIdAndActiveFlagTrue(
            Long resourceId, UUID subSkillId);

    boolean existsByResourceIdAndSubSkillId(
            Long resourceId, UUID subSkillId);

    @Query("SELECT rss FROM ResourceSubSkill rss WHERE rss.resourceId = :resourceId AND rss.activeFlag = true")
    List<ResourceSubSkill> findByResourceIdAndActiveFlagTrue(@Param("resourceId") Long resourceId);

    @Query("SELECT rss FROM ResourceSubSkill rss WHERE rss.resourceId = :resourceId AND rss.subSkillId IN :subSkillIds AND rss.activeFlag = true")
    List<ResourceSubSkill> findByResourceIdAndSubSkillIdsAndActiveFlagTrue(
            @Param("resourceId") Long resourceId, 
            @Param("subSkillIds") List<UUID> subSkillIds);
}
