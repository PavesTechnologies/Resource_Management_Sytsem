package com.repo.timeline_repo;

import com.dto.ResourceTimelineDTO;
import com.entity.resource_entities.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceTimelineRepository extends JpaRepository<Resource, Long> {

    @Query(value = """
        SELECT 
            r.resource_id as id,
            r.full_name as name,
            CONCAT(UPPER(SUBSTRING(r.full_name, 1, 1)), 
                   UPPER(COALESCE(SUBSTRING(SUBSTRING_INDEX(r.full_name, ' ', -1), 1, 1), ''))) as avatar,
            r.designation as role,
            r.working_location as location,
            COALESCE(r.experiance, 0) as experience,
            r.employment_type as employmentType,
            COALESCE(latest_ledger.confirmed_alloc_hours * 100.0 / NULLIF(latest_ledger.standard_hours, 0), 0) as currentAllocation,
            next_available.available_from as availableFrom,
            current_proj.project_name as currentProject,
            next_assign.project_name as nextAssignment
        FROM resource r
        LEFT JOIN (
            SELECT 
                resource_id,
                confirmed_alloc_hours,
                standard_hours
            FROM resource_availability_ledger 
            WHERE period_start <= CURDATE() 
            AND period_end >= CURDATE()
            ORDER BY period_start DESC 
            LIMIT 1
        ) latest_ledger ON r.resource_id = latest_ledger.resource_id
        LEFT JOIN (
            SELECT 
                ra.resource_id,
                p.name as project_name
            FROM resource_allocation ra
            JOIN project p ON ra.project_id = p.pms_project_id
            WHERE ra.allocation_status = 'ACTIVE'
            AND CURDATE() BETWEEN ra.allocation_start_date AND ra.allocation_end_date
            ORDER BY ra.allocation_percentage DESC
            LIMIT 1
        ) current_proj ON r.resource_id = current_proj.resource_id
        LEFT JOIN (
            SELECT 
                ra.resource_id,
                p.name as project_name,
                ra.allocation_start_date
            FROM resource_allocation ra
            JOIN project p ON ra.project_id = p.pms_project_id
            WHERE ra.allocation_status IN ('ACTIVE', 'PLANNED')
            AND ra.allocation_start_date > CURDATE()
            ORDER BY ra.allocation_start_date ASC
            LIMIT 1
        ) next_assign ON r.resource_id = next_assign.resource_id
        LEFT JOIN (
            SELECT 
                resource_id,
                MIN(period_start) as available_from
            FROM resource_availability_ledger
            WHERE period_start > CURDATE()
            AND firm_available_hours > 0
            GROUP BY resource_id
        ) next_available ON r.resource_id = next_available.resource_id
        WHERE r.active_flag = true
        ORDER BY r.full_name
        """, nativeQuery = true)
    List<Object[]> getResourceTimelineSummary();

    @Query(value = """
        SELECT 
            p.name as project,
            ra.allocation_start_date as startDate,
            ra.allocation_end_date as endDate,
            ra.allocation_percentage as allocation,
            CASE WHEN ra.allocation_status = 'PLANNED' THEN true ELSE false END as tentative
        FROM resource_allocation ra
        JOIN project p ON ra.project_id = p.pms_project_id
        WHERE ra.resource_id = :resourceId
        AND ra.allocation_status IN ('ACTIVE', 'PLANNED')
        ORDER BY ra.allocation_start_date ASC
        """, nativeQuery = true)
    List<Object[]> getAllocationTimeline(@Param("resourceId") Long resourceId);

    @Query(value = """
        SELECT 
            p.name as project_name
        FROM resource_allocation ra
        JOIN project p ON ra.project_id = p.pms_project_id
        WHERE ra.resource_id = :resourceId
        AND ra.allocation_status IN ('ACTIVE', 'PLANNED')
        AND (
            (ra.allocation_status = 'ACTIVE' AND CURDATE() BETWEEN ra.allocation_start_date AND ra.allocation_end_date)
            OR (ra.allocation_status = 'PLANNED' AND ra.allocation_start_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY))
        )
        ORDER BY ra.allocation_percentage DESC
        """, nativeQuery = true)
    List<Object[]> getCurrentProjects(@Param("resourceId") Long resourceId);

    @Query(value = """
        SELECT 
            ROUND(COALESCE(confirmed_alloc_hours * 100.0 / NULLIF(standard_hours, 0), 0)) as utilization
        FROM resource_availability_ledger
        WHERE resource_id = :resourceId
        AND period_start >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
        AND period_start <= CURDATE()
        ORDER BY period_start ASC
        """, nativeQuery = true)
    List<Integer> getUtilizationHistory(@Param("resourceId") Long resourceId);

    @Query(value = """
        SELECT 
            GROUP_CONCAT(DISTINCT s.skill_name SEPARATOR ', ') as skills
        FROM resource_skill rs
        JOIN skill s ON rs.skill_id = s.skill_id
        WHERE rs.resource_id = :resourceId
        AND rs.active_flag = true
        AND s.active_flag = true
        """, nativeQuery = true)
    Optional<String> getResourceSkills(@Param("resourceId") Long resourceId);
}
