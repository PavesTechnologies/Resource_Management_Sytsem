package com.repo.company_repo;

import com.entity.company_entities.CompanyEscalationContact;
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
}
