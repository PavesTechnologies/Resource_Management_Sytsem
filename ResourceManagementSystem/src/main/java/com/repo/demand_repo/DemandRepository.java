package com.repo.demand_repo;

import com.entity.demand_entities.Demand;
import com.entity_enums.demand_enums.DemandCommitment;
import com.entity_enums.demand_enums.DemandStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DemandRepository extends JpaRepository<Demand, UUID> {
    List<Demand> findByProject_PmsProjectId(Long pmsProjectId);
    
    @Query("SELECT d FROM Demand d WHERE d.project.pmsProjectId = :pmsProjectId AND d.project.resourceManagerId = :resourceManagerId")
    List<Demand> findByProject_PmsProjectIdAndResourceManagerId(@Param("pmsProjectId") Long pmsProjectId, @Param("resourceManagerId") Long resourceManagerId);
    
    @Query("SELECT d FROM Demand d WHERE d.project.resourceManagerId = :resourceManagerId")
    List<Demand> findByProjectResourceManagerId(@Param("resourceManagerId") Long resourceManagerId);
    
    @Query("SELECT d FROM Demand d WHERE d.createdBy = :createdBy")
    List<Demand> findByCreatedBy(@Param("createdBy") Long createdBy);
    
    @Query("SELECT d FROM Demand d WHERE d.createdBy = :createdBy AND d.project.pmsProjectId = :projectId")
    List<Demand> findByCreatedByAndProjectId(@Param("createdBy") Long createdBy, @Param("projectId") Long projectId);
    
    @Query("SELECT d FROM Demand d WHERE d.project.projectManagerId = :projectManagerId OR d.createdBy = :projectManagerId")
    List<Demand> findByProjectManagerIdOrCreatedBy(@Param("projectManagerId") Long projectManagerId);

    @Query("SELECT d FROM Demand d WHERE d.project.pmsProjectId = :projectId AND (d.project.projectManagerId = :projectManagerId OR d.createdBy = :projectManagerId)")
    List<Demand> findByProjectIdAndProjectManagerIdOrCreatedBy(@Param("projectId") Long projectId, @Param("projectManagerId") Long projectManagerId);

    @Query("SELECT d FROM Demand d WHERE d.project.pmsProjectId = :projectId")
    List<Demand> findByProjectId(@Param("projectId") Long projectId);
    
    List<Demand> findByDemandCommitmentAndCreatedAtBefore(DemandCommitment demandCommitment, LocalDateTime cutoffDate);
    
    // ============================
    // CONFLICT DETECTION QUERIES
    // ============================
    
    // Find overlapping demands for same project and role
    @Query("SELECT d FROM Demand d WHERE d.project.pmsProjectId = :projectId AND d.role.id = :roleId AND " +
           "d.demandStartDate <= :endDate AND d.demandEndDate >= :startDate AND " +
           "d.demandStatus NOT IN (:excludedStatuses)")
    List<Demand> findOverlappingDemands(@Param("projectId") Long projectId, 
                                       @Param("roleId") Long roleId, 
                                       @Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate,
                                       @Param("excludedStatuses") List<DemandStatus> excludedStatuses);
    
    // Find exact duplicate demands
    @Query("SELECT d FROM Demand d WHERE d.project.pmsProjectId = :projectId AND d.role.id = :roleId AND " +
           "d.demandStartDate = :startDate AND d.demandEndDate = :endDate AND " +
           "d.allocationPercentage = :allocationPercentage")
    List<Demand> findExactDuplicates(@Param("projectId") Long projectId, 
                                    @Param("roleId") Long roleId, 
                                    @Param("startDate") LocalDate startDate, 
                                    @Param("endDate") LocalDate endDate,
                                    @Param("allocationPercentage") Integer allocationPercentage);
    
    // Calculate total allocation for overlapping demands
    @Query("SELECT COALESCE(SUM(d.allocationPercentage), 0) FROM Demand d WHERE " +
           "d.project.pmsProjectId = :projectId AND d.demandStartDate <= :endDate AND " +
           "d.demandEndDate >= :startDate AND d.demandStatus NOT IN (:excludedStatuses)")
    Integer calculateTotalAllocation(@Param("projectId") Long projectId, 
                                   @Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate,
                                   @Param("excludedStatuses") List<DemandStatus> excludedStatuses);
    
    // Find demands requiring additional approval
    @Query("SELECT d FROM Demand d WHERE d.project.pmsProjectId = :projectId AND d.requiresAdditionalApproval = true")
    List<Demand> findDemandsRequiringApproval(@Param("projectId") Long projectId);
    
    // Count demands by status for project
    @Query("SELECT COUNT(d) FROM Demand d WHERE d.project.pmsProjectId = :projectId AND d.demandStatus = :status")
    Long countDemandsByProjectAndStatus(@Param("projectId") Long projectId, @Param("status") DemandStatus status);
    
    // Find high priority conflicting demands
    @Query("SELECT d FROM Demand d WHERE d.project.pmsProjectId = :projectId AND " +
           "d.demandStartDate <= :endDate AND d.demandEndDate >= :startDate AND " +
           "d.demandPriority IN :priorities AND d.demandStatus NOT IN (:excludedStatuses)")
    List<Demand> findHighPriorityConflictingDemands(@Param("projectId") Long projectId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   @Param("priorities") List<String> priorities,
                                                   @Param("excludedStatuses") List<DemandStatus> excludedStatuses);
    
    // Delivery Manager related queries
    @Query("SELECT d FROM Demand d WHERE d.project.deliveryOwnerId = :deliveryOwnerId")
    List<Demand> findByProjectDeliveryOwnerId(@Param("deliveryOwnerId") Long deliveryOwnerId);
    
    @Query("SELECT COUNT(d) FROM Demand d WHERE d.project.deliveryOwnerId = :deliveryOwnerId AND d.demandStatus = :status")
    Long countDemandsByDeliveryOwnerIdAndStatus(@Param("deliveryOwnerId") Long deliveryOwnerId, @Param("status") DemandStatus status);

    @Query("SELECT d FROM Demand d WHERE " +
           "d.demandStatus IN ('APPROVED')")
    List<Demand> findOpenDemands();
}
