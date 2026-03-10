package com.repo.skill_repo;

import com.entity.skill_entities.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

    boolean existsByNameIgnoreCaseAndCategory_Id(String name, UUID categoryId);

    List<Skill> findByStatusIgnoreCase(String status);

    List<Skill> findByCategory_IdAndStatusIgnoreCase(UUID categoryId, String status);

    @Query("SELECT s FROM Skill s JOIN FETCH s.category WHERE s.status = 'ACTIVE' ORDER BY s.name")
    List<Skill> findActiveSkills();

    @Query("SELECT COUNT(ss) FROM SubSkill ss WHERE ss.skill.id = :skillId AND ss.status = 'ACTIVE'")
    long countActiveSubSkillsBySkillId(@Param("skillId") UUID skillId);

    @Modifying
    @Transactional
    @Query("UPDATE Skill s SET s.status = 'INACTIVE' WHERE s.id = :skillId")
    int deactivateSkill(@Param("skillId") UUID skillId);

    @Query("SELECT s FROM Skill s JOIN FETCH s.category WHERE s.category.id = :categoryId AND s.status = 'ACTIVE' ORDER BY s.name")
    List<Skill> findActiveSkillsByCategoryId(@Param("categoryId") UUID categoryId);

    /**
     * Find skill by name for skill gap matching
     */
    @Query("SELECT s FROM Skill s WHERE s.name = :name")
    Optional<Skill> findByName(@Param("name") String name);

    // Commented out - skillType field doesn't exist in Skill entity
    // List<Skill> findBySkillTypeIgnoreCaseAndStatusIgnoreCase(String certification, String active);

    // Commented out - skillType field doesn't exist in Skill entity
    // List<Skill> findBySkillTypeIgnoreCaseAndStatusIgnoreCaseAndCategory_Id(String certification, String active, UUID categoryId);
}

