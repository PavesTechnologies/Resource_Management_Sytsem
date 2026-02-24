package com.repo.client_repo;

import com.entity.client_entities.ClientEscalationContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientContactRepo extends JpaRepository<ClientEscalationContact,UUID> {
    Optional<List<ClientEscalationContact>> findAllByClient_ClientId(UUID clientId);

    @Query("""
       SELECT c FROM ClientEscalationContact c
       WHERE c.client.clientId = :clientId
       ORDER BY 
           CASE 
               WHEN c.escalationLevel = 'Level-1' THEN 1
               WHEN c.escalationLevel = 'Level-2' THEN 2
               WHEN c.escalationLevel = 'Level-3' THEN 3
               ELSE 4
           END
       """)
    List<ClientEscalationContact> findContactsByClientOrdered(@Param("clientId") UUID clientId);
}
