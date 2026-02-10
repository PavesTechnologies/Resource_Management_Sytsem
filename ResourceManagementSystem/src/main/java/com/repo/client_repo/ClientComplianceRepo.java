package com.repo.client_repo;

import com.entity.client_entities.ClientCompliance;
import com.entity_enums.client_enums.RequirementType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientComplianceRepo extends JpaRepository<ClientCompliance, UUID> {
    Optional<List<ClientCompliance>> findAllByClient_ClientId(UUID clientId);
    Optional<ClientCompliance> findByClient_ClientIdAndRequirementType(UUID clientId, RequirementType requirementType);
}
