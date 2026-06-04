package com.repo.client_repo;

import com.entity.client_entities.ClientEscalationContact;
import com.entity_enums.client_enums.ContactRole;
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

    boolean existsByEmailAndClient_ClientId(String email, UUID clientId);

    boolean existsByContactNameAndClient_ClientId(String contactName, UUID clientId);

    @Query("""
       SELECT c FROM ClientEscalationContact c
       WHERE c.client.clientId = :clientId
       AND c.email = :email
       AND c.contactId != :excludeId
       """)
    Optional<ClientEscalationContact> findByEmailAndClientIdExcludingId(
            @Param("email") String email, 
            @Param("clientId") UUID clientId,
            @Param("excludeId") UUID excludeId);

    @Query("""
       SELECT c FROM ClientEscalationContact c
       WHERE c.client.clientId = :clientId
       AND c.contactName = :contactName
       AND c.contactId != :excludeId
       """)
    Optional<ClientEscalationContact> findByContactNameAndClientIdExcludingId(
            @Param("contactName") String contactName, 
            @Param("clientId") UUID clientId,
            @Param("excludeId") UUID excludeId);

    boolean existsByClient_ClientIdAndContactRoleAndEscalationLevel(UUID clientId, ContactRole contactRole, String escalationLevel);

    @Query("""
       SELECT c FROM ClientEscalationContact c
       WHERE c.client.clientId = :clientId
       AND c.contactRole = :contactRole
       AND c.escalationLevel = :escalationLevel
       AND c.contactId != :excludeId
       """)
    Optional<ClientEscalationContact> findByClient_ClientIdAndContactRoleAndEscalationLevelExcludingId(
            @Param("clientId") UUID clientId,
            @Param("contactRole") ContactRole contactRole,
            @Param("escalationLevel") String escalationLevel,
            @Param("excludeId") UUID excludeId);

    boolean existsByPhoneAndClient_ClientId(String phone, UUID clientId);

    @Query("""
       SELECT c FROM ClientEscalationContact c
       WHERE c.client.clientId = :clientId
       AND c.phone = :phone
       AND c.contactId != :excludeId
       """)
    Optional<ClientEscalationContact> findByPhoneAndClientIdExcludingId(
            @Param("phone") String phone,
            @Param("clientId") UUID clientId,
            @Param("excludeId") UUID excludeId);
}
