package com.repo.skill_repo;

import com.entity.skill_entities.DeliveryRoleExpectation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryRoleExpectationRepository extends JpaRepository<DeliveryRoleExpectation, UUID> {

    boolean existsByRoleNameAndSkill_IdAndSubSkill_Id(
            String roleName, UUID skillId, UUID subSkillId
    );

    boolean existsByRoleNameAndSkill_IdAndSubSkill_IdIsNull(
            String roleName, UUID skillId
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
}
