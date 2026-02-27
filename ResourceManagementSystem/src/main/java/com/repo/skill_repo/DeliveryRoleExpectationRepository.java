package com.repo.skill_repo;

import com.entity.skill_entities.DeliveryRoleExpectation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryRoleExpectationRepository extends JpaRepository<DeliveryRoleExpectation, UUID> {

    @Query("SELECT COUNT(dre) > 0 FROM DeliveryRoleExpectation dre WHERE dre.roleName = :roleName AND dre.skill.id = :skillId AND dre.subSkill.id = :subSkillId AND dre.status = 'ACTIVE'")
    boolean existsByRoleNameAndSkill_IdAndSubSkill_Id(
            @Param("roleName") String roleName, 
            @Param("skillId") UUID skillId, 
            @Param("subSkillId") UUID subSkillId
    );

    @Query("SELECT COUNT(dre) > 0 FROM DeliveryRoleExpectation dre WHERE dre.roleName = :roleName AND dre.skill.id = :skillId AND dre.subSkill IS NULL AND dre.status = 'ACTIVE'")
    boolean existsByRoleNameAndSkill_IdAndSubSkill_IdIsNull(
            @Param("roleName") String roleName, 
            @Param("skillId") UUID skillId
    );

    @Query("SELECT dre FROM DeliveryRoleExpectation dre WHERE dre.roleName = :roleName AND dre.status = 'ACTIVE' ORDER BY dre.skill.name, dre.subSkill.name")
    List<DeliveryRoleExpectation> findByRoleNameAndStatus(@Param("roleName") String roleName);

    @Query("SELECT DISTINCT dre.roleName FROM DeliveryRoleExpectation dre WHERE dre.status = 'ACTIVE' ORDER BY dre.roleName")
    List<String> findDistinctRoleNames();

    @Query("SELECT dre FROM DeliveryRoleExpectation dre WHERE dre.status = 'ACTIVE' ORDER BY dre.roleName, dre.skill.name, dre.subSkill.name")
    List<DeliveryRoleExpectation> findAllActive();

    @Query("SELECT dre FROM DeliveryRoleExpectation dre WHERE dre.roleName = :roleName AND dre.skill.id = :skillId AND dre.subSkill.id = :subSkillId AND dre.status = 'ACTIVE'")
    DeliveryRoleExpectation findByRoleNameAndSkillIdAndSubSkillId(
            @Param("roleName") String roleName,
            @Param("skillId") UUID skillId,
            @Param("subSkillId") UUID subSkillId
    );

    @Query("SELECT dre FROM DeliveryRoleExpectation dre WHERE dre.roleName = :roleName AND dre.skill.id = :skillId AND dre.subSkill IS NULL AND dre.status = 'ACTIVE'")
    DeliveryRoleExpectation findByRoleNameAndSkillIdAndSubSkillIsNull(
            @Param("roleName") String roleName,
            @Param("skillId") UUID skillId
    );

    @Query("SELECT COUNT(dre) FROM DeliveryRoleExpectation dre WHERE dre.roleName = :roleName AND dre.status = 'ACTIVE'")
    long countByRoleNameAndStatus(@Param("roleName") String roleName);

    @Query("SELECT dre FROM DeliveryRoleExpectation dre WHERE dre.roleName = :roleName AND dre.status = 'ACTIVE' AND dre.mandatoryFlag = true ORDER BY dre.skill.name, dre.subSkill.name")
    List<DeliveryRoleExpectation> findMandatoryExpectationsByRoleName(@Param("roleName") String roleName);

    @Query("SELECT dre FROM DeliveryRoleExpectation dre WHERE dre.roleName = :roleName AND dre.status = 'ACTIVE' AND dre.mandatoryFlag = false ORDER BY dre.skill.name, dre.subSkill.name")
    List<DeliveryRoleExpectation> findOptionalExpectationsByRoleName(@Param("roleName") String roleName);

    /**
     * Find delivery role expectation by ID with all related entities fetched
     * Optimized for skill gap matching to avoid N+1 queries
     */
    @Query("SELECT dre FROM DeliveryRoleExpectation dre " +
           "LEFT JOIN FETCH dre.skill " +
           "LEFT JOIN FETCH dre.subSkill " +
           "LEFT JOIN FETCH dre.proficiencyLevel " +
           "WHERE dre.id = :roleId")
    Optional<DeliveryRoleExpectation> findByIdWithDetails(@Param("roleId") UUID roleId);

    /**
     * Find all delivery role expectations by role name with details
     * Optimized for skill gap matching
     */
    @Query("SELECT dre FROM DeliveryRoleExpectation dre " +
           "LEFT JOIN FETCH dre.skill " +
           "LEFT JOIN FETCH dre.subSkill " +
           "LEFT JOIN FETCH dre.proficiencyLevel " +
           "WHERE dre.roleName = :roleName AND dre.status = 'ACTIVE'")
    List<DeliveryRoleExpectation> findByRoleNameWithDetails(@Param("roleName") String roleName);
}
