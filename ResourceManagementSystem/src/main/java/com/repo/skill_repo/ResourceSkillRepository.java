package com.repo.skill_repo;

import com.entity.skill_entities.ResourceSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceSkillRepository extends JpaRepository<ResourceSkill, UUID> {

    boolean existsByResourceIdAndSkillIdAndActiveFlagTrue(
            Long resourceId, UUID skillId);

    boolean existsByResourceIdAndSkillId(
            Long resourceId, UUID skillId);

    Optional<ResourceSkill>findByResourceIdAndSkillIdAndActiveFlagTrue(Long resourceId, UUID certSkillId);

    @Query("SELECT rs FROM ResourceSkill rs WHERE rs.resourceId = :resourceId AND rs.activeFlag = true")
    List<ResourceSkill> findByResourceIdAndActiveFlagTrue(@Param("resourceId") Long resourceId);

    List<ResourceSkill> findByActiveFlagTrue();
}
