package com.repo.report_repo;

import com.entity.resource_entities.Resource;
import com.entity.bench.ResourceState;
import com.entity.bench.ResourceCost;
import com.entity_enums.centralised_enums.RiskLevel;
import com.entity_enums.resource_enums.EmploymentStatus;
import com.entity_enums.bench.StateType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface BenchPoolReportRepository extends JpaRepository<Resource, Long> {

    @Query(value = """
        SELECT 
            r.resourceId as resourceId,
            r.fullName as name,
            r.employmentStatus as status,
            r.workingLocation as region,
            r.designation as role,
            COALESCE(rc.costPerDay * DATEDIFF(CURRENT_DATE, rs.benchStartDate), r.annualCtc / 365.0 * DATEDIFF(CURRENT_DATE, rs.benchStartDate), 0) as cost,
            rs.benchStartDate as lastAllocationEndDate,
            null as lastProject,
            null as client,
            s.name as skills,
            DATEDIFF(CURRENT_DATE, rs.benchStartDate) as benchDays
        FROM Resource r
        LEFT JOIN ResourceState rs ON r.resourceId = rs.resourceId AND rs.currentFlag = true
        LEFT JOIN ResourceCost rc ON r.resourceId = rc.resourceId 
            AND rc.effectiveFrom <= CURRENT_DATE 
            AND (rc.effectiveTo IS NULL OR rc.effectiveTo >= CURRENT_DATE)
        LEFT JOIN ResourceSkill rs2 ON r.resourceId = rs2.resourceId AND rs2.activeFlag = true
        LEFT JOIN Skill s ON rs2.skill.id = s.id
        WHERE r.employmentStatus = com.entity_enums.resource_enums.EmploymentStatus.ACTIVE
        AND rs.stateType = com.entity_enums.bench.StateType.BENCH
        AND rs.benchStartDate IS NOT NULL
        """)
    Page<Object[]> findBenchPoolReportData(Pageable pageable);
}
