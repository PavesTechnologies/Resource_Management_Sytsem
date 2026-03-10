package com.repo.allocation_repo;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.resource_entities.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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
           "WHERE ra.resource.resourceId = :resourceId")
    List<ResourceAllocation> findByResource_ResourceId(Long resourceId);
    
    @Query("SELECT ra FROM ResourceAllocation ra " +
           "LEFT JOIN FETCH ra.resource " +
           "LEFT JOIN FETCH ra.demand d " +
           "LEFT JOIN FETCH d.project p " +
           "LEFT JOIN FETCH p.client " +
           "LEFT JOIN FETCH ra.project proj " +
           "LEFT JOIN FETCH proj.client " +
           "WHERE ra.demand.demandId = :demandId")
    List<ResourceAllocation> findByDemand_DemandId(@Param("demandId") UUID demandId);
    
    @Query("SELECT ra FROM ResourceAllocation ra " +
           "LEFT JOIN FETCH ra.resource " +
           "LEFT JOIN FETCH ra.demand d " +
           "LEFT JOIN FETCH d.project p " +
           "LEFT JOIN FETCH p.client " +
           "LEFT JOIN FETCH ra.project proj " +
           "LEFT JOIN FETCH proj.client " +
           "WHERE ra.project.pmsProjectId = :projectId")
    List<ResourceAllocation> findByProject_PmsProjectId(@Param("projectId") Long projectId);

    @Query("SELECT ra.resource FROM ResourceAllocation ra WHERE ra.project.pmsProjectId = :projectId")
    List<Resource> findResourcesByProjectId(@Param("projectId") Long projectId);


    @Query("SELECT ra FROM ResourceAllocation ra " +
           "LEFT JOIN FETCH ra.resource " +
           "LEFT JOIN FETCH ra.demand d " +
           "LEFT JOIN FETCH d.project p " +
           "LEFT JOIN FETCH p.client " +
           "LEFT JOIN FETCH ra.project proj " +
           "LEFT JOIN FETCH proj.client " +
           "WHERE ra.resource.resourceId = :resourceId " +
           "AND ra.allocationStatus IN ('PLANNED', 'ACTIVE') " +
           "AND ra.allocationStartDate <= :endDate " +
           "AND ra.allocationEndDate >= :startDate")
    List<ResourceAllocation> findConflictingAllocations(
            @Param("resourceId") Long resourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    /**
     * Optimized batch query to fetch conflicting allocations for multiple resources
     * This query loads all overlapping allocations for the requested resources in a single 
     * round-trip to prevent N+1 queries and reduce allocation validation latency.
     * SQL logic filters overlapping allocations using:
     * existing.start_date <= request.end_date AND existing.end_date >= request.start_date
     */
    @Query("SELECT ra FROM ResourceAllocation ra " +
           "LEFT JOIN FETCH ra.resource " +
           "LEFT JOIN FETCH ra.demand d " +
           "LEFT JOIN FETCH d.project p " +
           "LEFT JOIN FETCH p.client " +
           "LEFT JOIN FETCH ra.project proj " +
           "LEFT JOIN FETCH proj.client " +
           "WHERE ra.resource.resourceId IN :resourceIds " +
           "AND ra.allocationStatus IN ('PLANNED', 'ACTIVE') " +
           "AND ra.allocationStartDate <= :endDate " +
           "AND ra.allocationEndDate >= :startDate")
    List<ResourceAllocation> findConflictingAllocationsForResources(
            @Param("resourceIds") List<Long> resourceIds,
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
           "AND ra.allocationStatus IN ('PLANNED', 'ACTIVE') " +
           "AND ra.allocationStartDate <= :date " +
           "AND ra.allocationEndDate >= :date")
    List<ResourceAllocation> findActiveAllocationsForResourceOnDate(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date);

    List<ResourceAllocation> findByOverrideFlagTrue();

    @Query("SELECT ra FROM ResourceAllocation ra " +
           "LEFT JOIN FETCH ra.resource " +
           "LEFT JOIN FETCH ra.demand d " +
           "LEFT JOIN FETCH d.project p " +
           "LEFT JOIN FETCH p.client " +
           "LEFT JOIN FETCH ra.project proj " +
           "LEFT JOIN FETCH proj.client " +
           "WHERE ra.allocationId IN :allocationIds")
    List<ResourceAllocation> findByAllocationIdIn(@Param("allocationIds") List<UUID> allocationIds);
}
