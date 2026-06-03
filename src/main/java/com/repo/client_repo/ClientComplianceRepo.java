package com.repo.client_repo;

import com.entity.client_entities.ClientCompliance;
import com.entity_enums.client_enums.RequirementType;
import com.entity.skill_entities.Certificate;
import com.entity.skill_entities.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientComplianceRepo extends JpaRepository<ClientCompliance, UUID> {
    Optional<List<ClientCompliance>> findAllByClient_ClientId(UUID clientId);
    Optional<ClientCompliance> findByClient_ClientIdAndRequirementType(UUID clientId, RequirementType requirementType);

    // Check for duplicate requirement name within the same client
    boolean existsByClient_ClientIdAndRequirementName(UUID clientId, String requirementName);

    // Check for duplicate certificate within the same client
    boolean existsByClient_ClientIdAndCertificate(UUID clientId, Certificate certificate);

    // Check for duplicate skill within the same client
    boolean existsByClient_ClientIdAndSkill(UUID clientId, Skill skill);

    // Find duplicate requirement name excluding current compliance
    @Query("""
       SELECT c FROM ClientCompliance c
       WHERE c.client.clientId = :clientId
       AND c.requirementName = :requirementName
       AND c.complianceId != :excludeId
       """)
    Optional<ClientCompliance> findByRequirementNameExcludingId(
            @Param("clientId") UUID clientId,
            @Param("requirementName") String requirementName,
            @Param("excludeId") UUID excludeId);

    // Find duplicate certificate excluding current compliance
    @Query("""
       SELECT c FROM ClientCompliance c
       WHERE c.client.clientId = :clientId
       AND c.certificate = :certificate
       AND c.complianceId != :excludeId
       """)
    Optional<ClientCompliance> findByCertificateExcludingId(
            @Param("clientId") UUID clientId,
            @Param("certificate") Certificate certificate,
            @Param("excludeId") UUID excludeId);

    // Find duplicate skill excluding current compliance
    @Query("""
       SELECT c FROM ClientCompliance c
       WHERE c.client.clientId = :clientId
       AND c.skill = :skill
       AND c.complianceId != :excludeId
       """)
    Optional<ClientCompliance> findBySkillExcludingId(
            @Param("clientId") UUID clientId,
            @Param("skill") Skill skill,
            @Param("excludeId") UUID excludeId);
}
