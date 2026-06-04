package com.repo.allocation_repo;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.resource_entities.Resource;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.allocation_enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface AllocationRepository extends JpaRepository<ResourceAllocation, UUID> {

    @Query("SELECT ra FROM ResourceAllocation ra " +
            "LEFT JOIN FETCH ra.resource " +
            "LEFT JOIN FETCH ra.demand d " +
            "LEFT JOIN FETCH d.project p " +
            "LEFT JOIN FETCH p.client " +
            "LEFT JOIN FETCH ra.project proj " +
            "LEFT JOIN FETCH proj.client " +
            "WHERE ra.resource.resourceId = :resourceId " +
            "AND ra.allocationStatus <> 'DELETED'")
    List<ResourceAllocation> findByResource_ResourceId(
            @Param("resourceId") String resourceId);

    @Query("SELECT ra FROM ResourceAllocation ra " +
           "WHERE ra.resource.resourceId = :resourceId " +
           "AND ra.allocationStatus = 'ACTIVE'")
    Optional<ResourceAllocation> findActiveByResourceId(@Param("resourceId") String resourceId);

    /**
     * Find all active allocations for a specific resource
     */
    @Query("SELECT ra FROM ResourceAllocation ra " +
           "WHERE ra.resource.resourceId = :resourceId " +
           "AND ra.allocationStatus = :status")
    List<ResourceAllocation> findByResource_ResourceIdAndAllocationStatus(
            @Param("resourceId") String resourceId, 
            @Param("status") AllocationStatus status);

    @Query("SELECT ra FROM ResourceAllocation ra " +
            "LEFT JOIN FETCH ra.resource " +
            "LEFT JOIN FETCH ra.demand d " +
            "LEFT JOIN FETCH d.project p " +
            "LEFT JOIN FETCH p.client " +
            "LEFT JOIN FETCH ra.project proj " +
            "LEFT JOIN FETCH proj.client " +
            "WHERE ra.demand.demandId = :demandId " +
            "AND ra.allocationStatus <> 'DELETED'")
    List<ResourceAllocation> findByDemand_DemandId(@Param("demandId") UUID demandId);

    @Query("SELECT ra FROM ResourceAllocation ra " +
            "LEFT JOIN FETCH ra.resource " +
            "LEFT JOIN FETCH ra.demand d " +
            "LEFT JOIN FETCH d.project p " +
            "LEFT JOIN FETCH p.client " +
            "LEFT JOIN FETCH ra.project proj " +
            "LEFT JOIN FETCH proj.client " +
            "WHERE ra.project.pmsProjectId = :projectId " +
            "AND ra.allocationStatus <> 'DELETED'")
    List<ResourceAllocation> findByProject_PmsProjectId(
            @Param("projectId") Long projectId);

    @Query("SELECT ra.resource FROM ResourceAllocation ra WHERE ra.project.pmsProjectId = :projectId AND ra.allocationStatus <> 'DELETED'")
    List<Resource> findResourcesByProjectId(@Param("projectId") Long projectId);

    @Query("""
    SELECT ra
    FROM ResourceAllocation ra
    WHERE ra.resource.resourceId = :resourceId
    AND ra.allocationStatus IN ('PLANNED', 'ACTIVE')
    AND ra.allocationStartDate <= :endDate
    AND ra.allocationEndDate >= :startDate
""")
    List<ResourceAllocation> findConflictingAllocations(
            @Param("resourceId") String resourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT ra FROM ResourceAllocation ra "+
           "WHERE ra.resource.resourceId IN :resourceIds " +
           "AND ra.allocationStatus IN ('PLANNED', 'ACTIVE') " +
           "AND ra.allocationStartDate <= :endDate " +
           "AND ra.allocationEndDate >= :startDate")
    List<ResourceAllocation> findConflictingAllocationsForResources(
            @Param("resourceIds") List<String> resourceIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    @Query("SELECT ra FROM ResourceAllocation ra " +
           "LEFT JOIN FETCH ra.resource " +
           "LEFT JOIN FETCH ra.demand d " +
           "LEFT JOIN FETCH d.project p " +
           "LEFT JOIN FETCH p.client " +
           "LEFT JOIN FETCH ra.project proj " +
           "LEFT JOIN FETCH proj.client " +
           "WHERE ra.resource.resourceId = :resourceId " +
           "AND ra.allocationStatus IN ('ACTIVE') " +
           "AND ra.allocationStartDate <= :date " +
           "AND ra.allocationEndDate >= :date")
    List<ResourceAllocation> findActiveAllocationsForResourceOnDate(
            @Param("resourceId") String resourceId,
            @Param("date") LocalDate date);

    @Query("SELECT ra FROM ResourceAllocation ra " +
            "WHERE ra.allocationStatus = 'ACTIVE' " +
            "AND ra.allocationEndDate < :today")
    List<ResourceAllocation> findExpiredAllocations(@Param("today") LocalDate today);

    @Modifying
    @Query("UPDATE ResourceAllocation ra SET ra.allocationStatus = :newStatus WHERE ra.allocationStatus = 'ACTIVE' AND ra.allocationEndDate < :today")
    int autoCloseAllocations(@Param("today") LocalDate today, @Param("newStatus") AllocationStatus newStatus);

    @Modifying
    @Query("UPDATE ResourceAllocation ra SET ra.allocationStatus = 'ACTIVE' WHERE ra.allocationStatus = 'PLANNED' AND ra.allocationStartDate <= :today")
    int activatePlannedAllocations(@Param("today") LocalDate today);

    @Query("SELECT ra FROM ResourceAllocation ra " +
            "LEFT JOIN FETCH ra.resource " +
            "LEFT JOIN FETCH ra.demand d " +
            "LEFT JOIN FETCH d.project p " +
            "LEFT JOIN FETCH p.client " +
            "LEFT JOIN FETCH ra.project proj " +
            "LEFT JOIN FETCH proj.client " +
            "WHERE ra.allocationId IN :allocationIds " +
            "AND ra.allocationStatus <> 'DELETED'")
    List<ResourceAllocation> findByAllocationIdIn(
            @Param("allocationIds") List<UUID> allocationIds);

    @Query("SELECT COUNT(ra) > 0 FROM ResourceAllocation ra " +
           "WHERE (ra.demand.project.client.clientId = :clientId OR ra.project.client.clientId = :clientId) " +
           "AND ra.allocationStatus IN ('ACTIVE', 'PLANNED')")
    boolean existsByClientIdAndActiveAllocation(@Param("clientId") UUID clientId);

    @Query("SELECT ra FROM ResourceAllocation ra " +
           "LEFT JOIN FETCH ra.resource " +
           "LEFT JOIN FETCH ra.demand d " +
           "LEFT JOIN FETCH d.project p " +
           "LEFT JOIN FETCH p.client " +
           "LEFT JOIN FETCH ra.project proj " +
           "LEFT JOIN FETCH proj.client " +
           "WHERE ra.resource.resourceId = :resourceId " +
            "AND ra.allocationStatus <> 'DELETED'" +
           "AND ra.allocationStartDate <= :date " +
           "AND ra.allocationEndDate >= :date")
    List<ResourceAllocation> findByResource_ResourceIdAndAllocationStartDateLessThanEqualAndAllocationEndDateGreaterThanEqual(
            @Param("resourceId") String resourceId,
            @Param("date") LocalDate date);

    List<ResourceAllocation>
    findAllByProject_PmsProjectIdAndResource_ResourceIdAndAllocationStatus(
            Long projectId,
            String resourceId,
            AllocationStatus status
    );

    Optional<ResourceAllocation>
    findByProject_PmsProjectIdAndResource_ResourceIdAndAllocationStatus(
            Long projectId,
            String resourceId,
            AllocationStatus status
    );

    @Query("""
        SELECT ra FROM ResourceAllocation ra
        WHERE ra.project.pmsProjectId = :projectId
        AND ra.allocationStatus = :status
    """)
    List<ResourceAllocation> findByProjectIdAndStatus(Long projectId, AllocationStatus status);

    List<ResourceAllocation> findByProject_PmsProjectIdAndResource_ResourceIdAndAllocationStatusAndAllocationEndDateAfter(
            Long projectId,
            String resourceId,
            AllocationStatus status,
            LocalDate endDate
    );

    @Query("SELECT DISTINCT ra.resource.resourceId FROM ResourceAllocation ra " +
           "WHERE ra.allocationStatus IN ('ACTIVE', 'APPROVED', 'PLANNED') " +
           "AND ra.allocationStartDate <= :endDate " +
           "AND ra.allocationEndDate >= :startDate")
    List<String> findResourcesWithAllocationsInDateRange(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    @Query("SELECT DISTINCT ra.resource.resourceId FROM ResourceAllocation ra " +
           "WHERE ra.allocationStatus IN ('ACTIVE', 'APPROVED', 'PLANNED') " +
           "AND ra.allocationStartDate <= :date " +
           "AND ra.allocationEndDate >= :date")
    Set<String> findActiveResourcesForDate(@Param("date") LocalDate date);

    @Query("SELECT MAX(ra.allocationEndDate) FROM ResourceAllocation ra " +
           "WHERE ra.resource.resourceId = :resourceId " +
           "AND ra.allocationStatus IN ('ACTIVE', 'APPROVED', 'PLANNED')")
    java.util.Optional<java.time.LocalDate> findMaxAllocationEndDateForResource(@Param("resourceId") String resourceId);

    @Query("SELECT MAX(ra.allocationEndDate) FROM ResourceAllocation ra " +
           "WHERE ra.resource.resourceId = :resourceId " +
           "AND ra.allocationStatus IN ('ACTIVE', 'APPROVED', 'PLANNED') " +
           "AND ra.allocationEndDate > :baseDate")
    java.util.Optional<java.time.LocalDate> findMaxAllocationEndDateAfter(@Param("resourceId") String resourceId,
                                                                          @Param("baseDate") LocalDate baseDate);

    @Query("SELECT MAX(ra.allocationStartDate) FROM ResourceAllocation ra " +
           "WHERE ra.resource.resourceId = :resourceId " +
           "AND ra.allocationStatus IN ('ACTIVE', 'APPROVED', 'PLANNED', 'ENDED')")
    java.util.Optional<java.time.LocalDate> findLastAllocationDateForResource(@Param("resourceId") String resourceId);

    @Query("""
    SELECT COALESCE(SUM(a.allocationPercentage), 0)
    FROM ResourceAllocation a
    WHERE a.resource.resourceId = :resourceId
    AND a.allocationStatus IN ('ACTIVE', 'PLANNED')
    AND :date BETWEEN a.allocationStartDate AND a.allocationEndDate
""")
    Integer getActiveAllocationPercentage(String resourceId, LocalDate date);

    List<ResourceAllocation> findByApprovalStatus(ApprovalStatus status);

    /**
     * Calculate average utilization percentage across all resources
     * Returns the average of total allocation percentages per resource
     * For each resource, sums all active allocations on current date, then averages across all resources
     */
    @Query(value = "SELECT AVG(resource_total.total_allocation) FROM (" +
           "SELECT SUM(ra.allocation_percentage) as total_allocation " +
           "FROM resource_allocation ra " +
           "WHERE ra.allocation_status = 'ACTIVE' " +
           "AND ra.allocation_start_date <= CURDATE() " +
           "AND ra.allocation_end_date >= CURDATE() " +
           "GROUP BY ra.resource_id" +
           ") resource_total", nativeQuery = true)
    Double calculateAverageUtilization();

    /**
     * Get total count of resources with active allocations
     */
    @Query("SELECT COUNT(DISTINCT ra.resource.resourceId) FROM ResourceAllocation ra " +
           "WHERE ra.allocationStatus = 'ACTIVE' " +
           "AND ra.allocationStartDate <= CURRENT_DATE " +
           "AND ra.allocationEndDate >= CURRENT_DATE")
    Long countActiveAllocatedResources();

    /**
     * Find PLANNED allocations that should be activated on or before a given date
     * Used by scheduler to auto-activate PLANNED allocations
     */
    @Query("SELECT ra FROM ResourceAllocation ra " +
           "WHERE ra.allocationType = 'PLANNED' " +
           "AND ra.allocationStatus = 'PLANNED' " +
           "AND ra.plannedStartDate IS NOT NULL " +
           "AND ra.plannedStartDate <= :date")
    List<ResourceAllocation> findPlannedAllocationsToActivate(@Param("date") LocalDate date);
}
