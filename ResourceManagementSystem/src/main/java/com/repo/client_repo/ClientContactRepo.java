package com.repo.client_repo;

import com.entity.client_entities.ClientEscalationContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientContactRepo extends JpaRepository<ClientEscalationContact,UUID> {
    Optional<List<ClientEscalationContact>> findAllByClient_ClientId(UUID clientId);
}
