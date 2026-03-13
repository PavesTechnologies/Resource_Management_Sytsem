package com.repo.allocation_repo;

import com.entity.allocation_entities.RoleOffEvent;
import com.entity_enums.allocation_enums.RoleOffReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RoleOffEventRepository extends JpaRepository<RoleOffEvent, Long> {

    /**
     * Find role-off events by specific reason for analysis
     */
    List<RoleOffEvent> findByRoleOffReason(RoleOffReason roleOffReason);

    /**
     * Find role-off events within a date range for reporting
     */
    @Query("SELECT roe FROM RoleOffEvent roe WHERE roe.roleOffDate BETWEEN :startDate AND :endDate")
    List<RoleOffEvent> findByRoleOffDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find role-off events by reason within date range for trend analysis
     */
    @Query("SELECT roe FROM RoleOffEvent roe WHERE roe.roleOffReason = :reason AND roe.roleOffDate BETWEEN :startDate AND :endDate")
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
    @Query("SELECT FUNCTION('YEAR', roe.roleOffDate), FUNCTION('MONTH', roe.roleOffDate), roe.roleOffReason, COUNT(roe) " +
            "FROM RoleOffEvent roe WHERE roe.roleOffDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('YEAR', roe.roleOffDate), FUNCTION('MONTH', roe.roleOffDate), roe.roleOffReason " +
            "ORDER BY FUNCTION('YEAR', roe.roleOffDate), FUNCTION('MONTH', roe.roleOffDate)")
    List<Object[]> getRoleOffTrendsByMonth(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Get role-off reasons by project for risk analysis
     */
    @Query("SELECT p.name, roe.roleOffReason, COUNT(roe) " +
            "FROM RoleOffEvent roe JOIN roe.project p " +
            "WHERE roe.roleOffDate BETWEEN :startDate AND :endDate " +
            "GROUP BY p.name, roe.roleOffReason " +
            "ORDER BY COUNT(roe) DESC")
    List<Object[]> getRoleOffReasonsByProject(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Get role-off reasons by client for delivery risk assessment
     */
    @Query("SELECT c.name, roe.roleOffReason, COUNT(roe) " +
            "FROM RoleOffEvent roe JOIN roe.project p JOIN p.client c " +
            "WHERE roe.roleOffDate BETWEEN :startDate AND :endDate " +
            "GROUP BY c.name, roe.roleOffReason " +
            "ORDER BY COUNT(roe) DESC")
    List<Object[]> getRoleOffReasonsByClient(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Get performance-related role-offs for quality metrics
     */
    @Query("SELECT roe FROM RoleOffEvent roe WHERE roe.roleOffReason = 'PERFORMANCE' AND roe.roleOffDate BETWEEN :startDate AND :endDate")
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
}
