package com.repo;

import com.entity.demand_entities.Demand;
import com.entity_enums.demand_enums.DemandCommitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    
    List<Demand> findByDemandCommitmentAndCreatedAtBefore(DemandCommitment demandCommitment, LocalDateTime cutoffDate);
}
