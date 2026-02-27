package com.repo.demand_repo;

import com.entity.demand_entities.DemandSLA;
import com.entity_enums.client_enums.SLAType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DemandSLARepository extends JpaRepository<DemandSLA, UUID> {

    List<DemandSLA> findByActiveFlagTrue();

    List<DemandSLA> findByDueAtBeforeAndActiveFlagTrue(LocalDate now);

    Optional<DemandSLA> findByDemand_DemandIdAndActiveFlagTrue(UUID demandId);
    @Modifying
    @Query("""
       UPDATE DemandSLA d
       SET d.activeFlag = false
       WHERE d.demand.demandId = :demandId
       AND d.activeFlag = true
       """)
    int deactivateByDemandId(@Param("demandId") UUID demandId);
}
