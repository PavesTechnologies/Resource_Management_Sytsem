package com.repo.allocation_repo;

import com.entity.allocation_entities.AllocationModification;
import com.entity_enums.allocation_enums.AllocationModificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AllocationModificationRepository extends JpaRepository<AllocationModification, UUID> {
    
    List<AllocationModification> findByAllocation_AllocationId(UUID allocationId);
    
    List<AllocationModification> findByRequestedBy(String requestedBy);
    
    List<AllocationModification> findByStatus(AllocationModificationStatus status);
    
    @Query("SELECT am FROM AllocationModification am WHERE am.allocation.project.resourceManagerId = :resourceManagerId")
    List<AllocationModification> findByResourceManagerId(@Param("resourceManagerId") Long resourceManagerId);
    
    @Query("SELECT am FROM AllocationModification am WHERE am.allocation.project.projectManagerId = :projectManagerId")
    List<AllocationModification> findByProjectManagerId(@Param("projectManagerId") Long projectManagerId);
    
    @Query("SELECT am FROM AllocationModification am WHERE am.allocation.allocationId = :allocationId AND am.effectiveDate = :effectiveDate AND am.status NOT IN (:excludedStatuses)")
    List<AllocationModification> findDuplicateModifications(@Param("allocationId") UUID allocationId, 
                                                          @Param("effectiveDate") LocalDate effectiveDate,
                                                          @Param("excludedStatuses") List<AllocationModificationStatus> excludedStatuses);
    
    @Query("SELECT am FROM AllocationModification am WHERE am.allocation.allocationId = :allocationId AND am.status = :status ORDER BY am.requestedAt DESC")
    List<AllocationModification> findByAllocationIdAndStatus(@Param("allocationId") UUID allocationId, 
                                                             @Param("status") AllocationModificationStatus status);
    
    @Query("SELECT COUNT(am) FROM AllocationModification am WHERE am.allocation.allocationId = :allocationId AND am.status = :status")
    Long countByAllocationIdAndStatus(@Param("allocationId") UUID allocationId, 
                                      @Param("status") AllocationModificationStatus status);
    
    @Query("SELECT am FROM AllocationModification am WHERE am.requestedBy = :requestedBy AND am.allocation.project.pmsProjectId = :projectId")
    List<AllocationModification> findByRequestedByAndProjectId(@Param("requestedBy") String requestedBy, 
                                                               @Param("projectId") Long projectId);
    
    @Query("SELECT am FROM AllocationModification am WHERE am.allocation.project.resourceManagerId = :resourceManagerId AND am.status IN (:statuses)")
    List<AllocationModification> findByResourceManagerIdAndStatuses(@Param("resourceManagerId") Long resourceManagerId, 
                                                                   @Param("statuses") List<AllocationModificationStatus> statuses);
}
