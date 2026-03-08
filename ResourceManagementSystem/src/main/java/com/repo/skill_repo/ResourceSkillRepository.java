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

    /**
     * Batch query to fetch skills for multiple resources in a single round-trip
     * This prevents N+1 query problems when validating skills for multiple resources
     */
    @Query("SELECT rs FROM ResourceSkill rs WHERE rs.resourceId IN :resourceIds AND rs.activeFlag = true")
    List<ResourceSkill> findByResourceIdInAndActiveFlagTrue(@Param("resourceIds") List<Long> resourceIds);

    @Query("SELECT rs.resourceId, s.name FROM ResourceSkill rs JOIN rs.skill s WHERE rs.resourceId IN :resourceIds AND rs.activeFlag = true")
    List<Object[]> findResourceIdAndSkillNames(@Param("resourceIds") List<Long> resourceIds);

    @Query("SELECT rs.resourceId, s.name, pl.proficiencyName, rs.lastUsedDate FROM ResourceSkill rs JOIN rs.skill s JOIN com.entity.skill_entities.ProficiencyLevel pl ON rs.proficiencyId = pl.proficiencyId WHERE rs.resourceId IN :resourceIds AND rs.activeFlag = true")
    List<Object[]> findResourceIdAndSkillDetails(@Param("resourceIds") List<Long> resourceIds);

    List<ResourceSkill> findByActiveFlagTrue();
}
