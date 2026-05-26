package com.repo.skill_repo;

import com.entity.skill_entities.EmployeeSkillMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeSkillMappingRepository extends JpaRepository<EmployeeSkillMapping, UUID> {

    boolean existsByEmployeeIdAndSkill_IdAndSubSkill_Id(String employeeId, UUID skillId, UUID subSkillId);

    @Query("SELECT esm FROM EmployeeSkillMapping esm " +
           "JOIN FETCH esm.category " +
           "JOIN FETCH esm.skill " +
           "LEFT JOIN FETCH esm.subSkill " +
           "LEFT JOIN FETCH esm.proficiency " +
           "WHERE esm.employeeId = :employeeId AND esm.status = 'ACTIVE' " +
           "ORDER BY esm.category.name, esm.skill.name")
    List<EmployeeSkillMapping> findActiveSkillsByEmployeeId(@Param("employeeId") String employeeId);

    @Query("SELECT esm FROM EmployeeSkillMapping esm " +
           "WHERE esm.employeeId = :employeeId " +
           "AND esm.skill.id = :skillId " +
           "AND ((:subSkillId IS NULL AND esm.subSkill IS NULL) OR esm.subSkill.id = :subSkillId)")
    Optional<EmployeeSkillMapping> findExistingMapping(@Param("employeeId") String employeeId,
                                                       @Param("skillId") UUID skillId,
                                                       @Param("subSkillId") UUID subSkillId);
}
