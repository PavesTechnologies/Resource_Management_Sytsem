package com.repo.client_repo;

import com.entity.client_entities.ClientCompliance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientComplianceRepo extends JpaRepository<ClientCompliance, Long> {
    Optional<List<ClientCompliance>> findAllByClient_ClientId(Long clientId);
}
