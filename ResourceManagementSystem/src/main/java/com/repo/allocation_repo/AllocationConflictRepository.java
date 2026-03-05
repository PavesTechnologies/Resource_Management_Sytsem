package com.repo.allocation_repo;

import com.entity.allocation_entities.AllocationConflict;
import com.entity.resource_entities.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AllocationConflictRepository extends JpaRepository<AllocationConflict, UUID> {

    List<AllocationConflict> findByResource_ResourceId(Long resourceId);
    
    List<AllocationConflict> findByResource_ResourceIdAndResolutionStatus(Long resourceId, String resolutionStatus);
    
    List<AllocationConflict> findByResolutionStatus(String resolutionStatus);
    
    List<AllocationConflict> findByConflictType(String conflictType);
    
    @Query("SELECT ac FROM AllocationConflict ac WHERE ac.resource = :resource " +
           "AND ac.resolutionStatus = 'PENDING' " +
           "ORDER BY ac.conflictSeverity DESC, ac.detectedAt ASC")
    List<AllocationConflict> findPendingConflictsForResource(@Param("resource") Resource resource);
    
    @Query("SELECT ac FROM AllocationConflict ac WHERE ac.resolutionStatus = 'PENDING' " +
           "ORDER BY ac.conflictSeverity DESC, ac.detectedAt ASC")
    List<AllocationConflict> findAllPendingConflicts();
    
    @Query("SELECT COUNT(ac) FROM AllocationConflict ac WHERE ac.resolutionStatus = 'PENDING'")
    long countPendingConflicts();
    
    @Query("SELECT COUNT(ac) FROM AllocationConflict ac WHERE ac.conflictSeverity = 'HIGH' AND ac.resolutionStatus = 'PENDING'")
    long countHighSeverityPendingConflicts();
}
