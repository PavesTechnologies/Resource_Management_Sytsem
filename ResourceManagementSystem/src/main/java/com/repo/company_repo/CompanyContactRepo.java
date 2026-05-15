package com.repo.company_repo;

import com.entity.company_entities.CompanyEscalationContact;
import com.entity_enums.client_enums.ContactRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyContactRepo extends JpaRepository<CompanyEscalationContact, UUID> {
    Optional<List<CompanyEscalationContact>> findAllByCompany_CompanyId(UUID companyId);

    @Query("""
       SELECT c FROM CompanyEscalationContact c
       WHERE c.company.companyId = :companyId
       ORDER BY 
           CASE 
               WHEN c.escalationLevel = 'Level-1' THEN 1
               WHEN c.escalationLevel = 'Level-2' THEN 2
               WHEN c.escalationLevel = 'Level-3' THEN 3
               ELSE 4
           END
       """)
    List<CompanyEscalationContact> findContactsByCompanyOrdered(@Param("companyId") UUID companyId);

    boolean existsByEmailAndCompany_CompanyId(String email, UUID companyId);

    boolean existsByContactNameAndCompany_CompanyId(String contactName, UUID companyId);

    @Query("""
       SELECT c FROM CompanyEscalationContact c
       WHERE c.company.companyId = :companyId
       AND c.email = :email
       AND c.contactId != :excludeId
       """)
    Optional<CompanyEscalationContact> findByEmailAndCompanyIdExcludingId(
            @Param("email") String email, 
            @Param("companyId") UUID companyId,
            @Param("excludeId") UUID excludeId);

    @Query("""
       SELECT c FROM CompanyEscalationContact c
       WHERE c.company.companyId = :companyId
       AND c.contactName = :contactName
       AND c.contactId != :excludeId
       """)
    Optional<CompanyEscalationContact> findByContactNameAndCompanyIdExcludingId(
            @Param("contactName") String contactName, 
            @Param("companyId") UUID companyId,
            @Param("excludeId") UUID excludeId);

    // Instead of COUNT(c) > 0 (invalid boolean JPQL), use COUNT(c)
    @Query("""
   SELECT COUNT(c) FROM CompanyEscalationContact c
   WHERE c.company.companyId = :companyId
   AND c.contactRole = :contactRole
   AND c.escalationLevel = :escalationLevel
   """)
    Long countByCompanyIdAndContactRoleAndEscalationLevel(
            @Param("companyId") UUID companyId,
            @Param("contactRole") ContactRole contactRole,
            @Param("escalationLevel") String escalationLevel);

    // Find duplicate contact role and escalation level combination excluding current contact
    @Query("""
       SELECT c FROM CompanyEscalationContact c
       WHERE c.company.companyId = :companyId
       AND c.contactRole = :contactRole
       AND c.escalationLevel = :escalationLevel
       AND c.contactId != :excludeId
       """)
    Optional<CompanyEscalationContact> findByCompanyIdAndContactRoleAndEscalationLevelExcludingId(
            @Param("companyId") UUID companyId,
            @Param("contactRole") ContactRole contactRole,
            @Param("escalationLevel") String escalationLevel,
            @Param("excludeId") UUID excludeId);

    boolean existsByPhoneAndCompany_CompanyId(String phone, UUID companyId);

    @Query("""
       SELECT c FROM CompanyEscalationContact c
       WHERE c.company.companyId = :companyId
       AND c.phone = :phone
       AND c.contactId != :excludeId
       """)
    Optional<CompanyEscalationContact> findByPhoneAndCompanyIdExcludingId(
            @Param("phone") String phone,
            @Param("companyId") UUID companyId,
            @Param("excludeId") UUID excludeId);
}
