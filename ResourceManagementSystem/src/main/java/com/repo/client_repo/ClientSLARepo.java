package com.repo.client_repo;

import com.entity.client_entities.ClientSLA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientSLARepo extends JpaRepository<ClientSLA,UUID> {
    Optional<List<ClientSLA>> findAllByClient_ClientId(UUID clientId);
}
