package com.repo.roleoff_repo;

import com.dto.roleoff_dto.ProjectRoleOffKPIDTO;
import com.dto.roleoff_dto.ResourcesDTO;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.allocation_entities.RoleOffEvent;
import com.entity.resource_entities.Resource;
import com.entity_enums.allocation_enums.RoleOffReason;
import com.entity_enums.allocation_enums.RoleOffStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import java.util.UUID;

@Repository
public interface RoleOffEventRepository extends JpaRepository<RoleOffEvent, UUID> {
    boolean existsByAllocation_AllocationId(UUID allocationId);

        /**
         * Find role-off events by specific reason for analysis
         */
        List<RoleOffEvent> findByRoleOffReason(RoleOffReason roleOffReason);

        /**
         * Find role-off events within a date range for reporting
         */
        @Query("SELECT roe FROM RoleOffEvent roe WHERE roe.effectiveRoleOffDate BETWEEN :startDate AND :endDate")
        List<RoleOffEvent> findByRoleOffDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * Find role-off events by reason within date range for trend analysis
         */
        @Query("SELECT roe FROM RoleOffEvent roe WHERE roe.roleOffReason = :reason AND roe.effectiveRoleOffDate BETWEEN :startDate AND :endDate")
        List<RoleOffEvent> findByRoleOffReasonAndDateBetween(
                @Param("reason") RoleOffReason reason,
                @Param("startDate") LocalDate startDate,
                @Param("endDate") LocalDate endDate
        );

        /**
         * Count role-off events by reason for statistics
         */
        @Query("SELECT roe.roleOffReason, COUNT(roe) FROM RoleOffEvent roe WHERE roe.roleOffReason IS NOT NULL GROUP BY roe.roleOffReason")
        List<Object[]> countByRoleOffReason();

        /**
         * Get role-off trends by month and reason
         */
        @Query("SELECT FUNCTION('YEAR', roe.effectiveRoleOffDate), FUNCTION('MONTH', roe.effectiveRoleOffDate), roe.roleOffReason, COUNT(roe) " +
                "FROM RoleOffEvent roe WHERE roe.effectiveRoleOffDate BETWEEN :startDate AND :endDate " +
                "GROUP BY FUNCTION('YEAR', roe.effectiveRoleOffDate), FUNCTION('MONTH', roe.effectiveRoleOffDate), roe.roleOffReason " +
                "ORDER BY FUNCTION('YEAR', roe.effectiveRoleOffDate), FUNCTION('MONTH', roe.effectiveRoleOffDate)")
        List<Object[]> getRoleOffTrendsByMonth(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * Get role-off reasons by project for risk analysis
         */
        @Query("SELECT p.name, roe.roleOffReason, COUNT(roe) " +
                "FROM RoleOffEvent roe JOIN roe.project p " +
                "WHERE roe.effectiveRoleOffDate BETWEEN :startDate AND :endDate " +
                "GROUP BY p.name, roe.roleOffReason " +
                "ORDER BY COUNT(roe) DESC")
        List<Object[]> getRoleOffReasonsByProject(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * Get role-off reasons by client for delivery risk assessment
         */
        @Query("SELECT c.clientName, roe.roleOffReason, COUNT(roe) " +
                "FROM RoleOffEvent roe JOIN roe.project p JOIN p.client c " +
                "WHERE roe.effectiveRoleOffDate BETWEEN :startDate AND :endDate " +
                "GROUP BY c.clientName, roe.roleOffReason " +
                "ORDER BY COUNT(roe) DESC")
        List<Object[]> getRoleOffReasonsByClient(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * Get performance-related role-offs for quality metrics
         */
        @Query("SELECT roe FROM RoleOffEvent roe WHERE roe.roleOffReason = 'PERFORMANCE' AND roe.effectiveRoleOffDate BETWEEN :startDate AND :endDate")
        List<RoleOffEvent> getPerformanceRelatedRoleOffs(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * Find role-off events for a specific project
         */
        List<RoleOffEvent> findByProject_PmsProjectId(Long pmsProjectId);

        /**
         * Find role-off events for a specific project (alternative method name for compatibility)
         */
        @Query("SELECT roe FROM RoleOffEvent roe WHERE roe.project.pmsProjectId = :projectId")
        List<RoleOffEvent> findByProject_ProjectId(@Param("projectId") Long projectId);

        /**
         * Find role-off events for a specific resource
         */
        List<RoleOffEvent> findByResource_ResourceId(Long resourceId);

        /**
         * Find role-off events with missing reason classification for governance
         */
        @Query("SELECT roe FROM RoleOffEvent roe WHERE roe.roleOffReason IS NULL")
        List<RoleOffEvent> findEventsWithMissingReason();

        /**
         * Count role-off events by reason for a specific client (via project)
         */
        @Query("SELECT roe.roleOffReason, COUNT(roe) FROM RoleOffEvent roe JOIN roe.project p JOIN p.client c WHERE c.clientId = :clientId AND roe.roleOffReason IS NOT NULL GROUP BY roe.roleOffReason")
        List<Object[]> countByRoleOffReasonForClient(@Param("clientId") UUID clientId);

        /**
         * Count role-off events within a date range for trend analysis
         */
        @Query("SELECT COUNT(roe) FROM RoleOffEvent roe WHERE roe.effectiveRoleOffDate BETWEEN :startDate AND :endDate")
        long countRoleOffsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * Count open demand positions for a project (for delivery impact validation)
         */
        @Query("SELECT COUNT(d) FROM Demand d WHERE d.project.pmsProjectId = :projectId AND d.demandStatus IN ('REQUESTED', 'APPROVED')")
        long countOpenDemandForProject(@Param("projectId") Long projectId);

        /**
         * Count active allocations for a project (for delivery impact validation)
         */
        @Query("SELECT COUNT(ra) FROM ResourceAllocation ra WHERE ra.project.pmsProjectId = :projectId AND ra.allocationStatus = 'ACTIVE'")
        long countActiveAllocationsForProject(@Param("projectId") Long projectId);

        /**
         * Get required positions for a project (for delivery impact validation)
         */
        @Query("SELECT d.resourcesRequired FROM Demand d WHERE d.project.pmsProjectId = :projectId AND d.demandStatus IN ('REQUESTED', 'APPROVED')")
        List<Integer> getRequiredPositionsForProject(@Param("projectId") Long projectId);

        /**
         * Check if project is critical based on priority level
         */
        @Query("SELECT p.priorityLevel FROM Project p WHERE p.pmsProjectId = :projectId")
        String getProjectPriorityLevel(@Param("projectId") Long projectId);

        /**
         * Get current utilization percentage for a resource
         */
        @Query("SELECT ra.allocationPercentage FROM ResourceAllocation ra WHERE ra.resource.resourceId = :resourceId AND ra.allocationStatus = 'ACTIVE'")
        List<Integer> getCurrentUtilization(@Param("resourceId") Long resourceId);

        /**
         * Get total allocation percentage for a resource
         */
        @Query("SELECT COALESCE(SUM(ra.allocationPercentage), 0) FROM ResourceAllocation ra WHERE ra.resource.resourceId = :resourceId AND ra.allocationStatus = 'ACTIVE'")
        Integer getTotalCurrentUtilization(@Param("resourceId") Long resourceId);

        /**
         * Get resource availability percentage (100 - current utilization)
         */
        @Query("SELECT (100 - COALESCE(SUM(ra.allocationPercentage), 0)) FROM ResourceAllocation ra WHERE ra.resource.resourceId = :resourceId AND ra.allocationStatus = 'ACTIVE'")
        Integer getResourceAvailability(@Param("resourceId") Long resourceId);

    /**
     * Find role-off events with effective date today for scheduler
     */
//    @Query("SELECT roe FROM RoleOffEvent roe WHERE roe.effectiveRoleOffDate = :today AND roe.roleOffStatus = 'APPROVED'")
//    List<RoleOffEvent> findApprovedRoleOffsForToday(@Param("today") LocalDate today);

    @Query("""
    SELECT ra FROM ResourceAllocation ra
    JOIN FETCH ra.resource r
    LEFT JOIN FETCH ra.project p
    LEFT JOIN FETCH p.client c
    LEFT JOIN FETCH ra.demand d
    WHERE p.pmsProjectId = :projectId
    AND p.projectManagerId = :managerId
    AND ra.allocationStatus = 'ACTIVE'
    """)
    List<ResourceAllocation> findResources(Long projectId, Long managerId);

    @Query("""
    SELECT new com.dto.roleoff_dto.ProjectRoleOffKPIDTO(
        (SELECT COUNT(ra) FROM ResourceAllocation ra 
         WHERE ra.project.pmsProjectId = :projectId 
         AND ra.allocationStatus = com.entity_enums.allocation_enums.AllocationStatus.ACTIVE),

        (SELECT COUNT(roe) FROM RoleOffEvent roe 
         WHERE roe.project.pmsProjectId = :projectId 
         AND roe.roleOffStatus <> com.entity_enums.allocation_enums.RoleOffStatus.FULFILLED),

        (SELECT COUNT(roe) FROM RoleOffEvent roe 
         WHERE roe.project.pmsProjectId = :projectId 
         AND roe.roleOffStatus = com.entity_enums.allocation_enums.RoleOffStatus.FULFILLED)
    )
""")
    ProjectRoleOffKPIDTO getProjectKPI(Long projectId);

        /**
         * Find role-off events with effective date today for scheduler
         */
        @Query("SELECT roe FROM RoleOffEvent roe WHERE roe.effectiveRoleOffDate <= :today AND roe.roleOffStatus = 'APPROVED'")
        List<RoleOffEvent> findApprovedRoleOffsForToday(@Param("today") LocalDate today);

        @Query("""
            SELECT r FROM RoleOffEvent r
            WHERE r.project.pmsProjectId IN (
                SELECT p.pmsProjectId FROM Project p WHERE p.resourceManagerId = :rmId
            )
            AND r.roleOffStatus = :status
        """)
        List<RoleOffEvent> findPendingRoleOffs(@Param("rmId") Long rmId, @Param("status") RoleOffStatus status);

    @Query("""
            SELECT r FROM RoleOffEvent r
            WHERE r.project.pmsProjectId IN (
                SELECT p.pmsProjectId FROM Project p WHERE p.deliveryOwnerId = :rmId
            )
            AND r.roleOffStatus = :status
        """)
    List<RoleOffEvent> findPendingRoleOffsDm(@Param("rmId") Long rmId, @Param("status") RoleOffStatus status);

    List<RoleOffEvent> findByProject_PmsProjectIdAndProjectProjectManagerId(Long projectId, Long projectManagerId);
}
