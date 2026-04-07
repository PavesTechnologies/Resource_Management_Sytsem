package com.repo.skill_repo;

import com.entity.skill_entities.ResourceSubSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ResourceSubSkillRepository extends JpaRepository<ResourceSubSkill, UUID> {

    List<ResourceSubSkill> findByResourceId(Long resourceId);

    boolean existsByResourceIdAndSubSkillIdAndActiveFlagTrue(
            Long resourceId, UUID subSkillId);

    boolean existsByResourceIdAndSubSkillId(
            Long resourceId, UUID subSkillId);

    boolean existsByResourceIdAndSubSkillIdAndIdNot(Long resourceId, UUID subSkillId, UUID id);

    @Query("SELECT rss FROM ResourceSubSkill rss WHERE rss.resourceId = :resourceId AND rss.activeFlag = true")
    List<ResourceSubSkill> findByResourceIdAndActiveFlagTrue(@Param("resourceId") Long resourceId);

    @Query("SELECT rss FROM ResourceSubSkill rss WHERE rss.resourceId = :resourceId AND rss.subSkill.id IN :subSkillIds AND rss.activeFlag = true")
    List<ResourceSubSkill> findByResourceIdAndSubSkillIdsAndActiveFlagTrue(
            @Param("resourceId") Long resourceId, 
            @Param("subSkillIds") List<UUID> subSkillIds);

    @Query("SELECT rss.resourceId, ss.name FROM ResourceSubSkill rss JOIN rss.subSkill ss WHERE rss.resourceId IN :resourceIds AND rss.activeFlag = true")
    List<Object[]> findResourceIdAndSubSkillNames(@Param("resourceIds") List<Long> resourceIds);

    @Query("SELECT rss.resourceId, ss.name, pl.proficiencyName, rss.lastUsedDate FROM ResourceSubSkill rss JOIN rss.subSkill ss JOIN com.entity.skill_entities.ProficiencyLevel pl ON rss.proficiencyId = pl.proficiencyId WHERE rss.resourceId IN :resourceIds AND rss.activeFlag = true")
    List<Object[]> findResourceIdAndSubSkillDetails(@Param("resourceIds") List<Long> resourceIds);

    @Query("""
    SELECT rss.resourceId, rss.subSkill.name
    FROM ResourceSubSkill rss
    WHERE rss.resourceId IN :resourceIds
    AND rss.activeFlag = true
""")
    List<Object[]> findSubSkillsByResourceIds(List<Long> resourceIds);

    @Query("SELECT rss FROM ResourceSubSkill rss LEFT JOIN FETCH rss.subSkill")
    List<ResourceSubSkill> findAllWithSubSkills();

    /**
     * Batch update lastUsedDate for resource subskills by skill IDs
     * Used during role-off to update lastUsedDate for project-specific subskills
     */
    @Query("UPDATE ResourceSubSkill rss SET rss.lastUsedDate = :effectiveDate " +
           "WHERE rss.resourceId = :resourceId " +
           "AND rss.subSkill.id IN :skillIds " +
           "AND rss.activeFlag = true")
    int updateLastUsedDateByResourceIdAndSkillIds(
            @Param("resourceId") Long resourceId, 
            @Param("skillIds") List<UUID> skillIds, 
            @Param("effectiveDate") LocalDate effectiveDate);
}
