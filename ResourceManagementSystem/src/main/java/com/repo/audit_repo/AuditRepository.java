package com.repo.audit_repo;

import com.entity.audit_entities.Audit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditRepository extends JpaRepository<Audit, UUID> {
    
    List<Audit> findByModuleOrderByTimestampDesc(String module);
    
    List<Audit> findByEntityOrderByTimestampDesc(String entity);
    
    List<Audit> findByUserOrderByTimestampDesc(String user);
    
    List<Audit> findByTransactionIdOrderByTimestampDesc(String transactionId);
}
