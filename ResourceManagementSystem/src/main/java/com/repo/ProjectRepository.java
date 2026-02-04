package com.repo;

import com.entity.Project;
import com.entity_enums.ProjectStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT COUNT(p) FROM Project p WHERE p.clientId = :clientId")
    Long countTotalProjectsByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.clientId = :clientId AND p.projectStatus = :status")
    Long countProjectsByClientIdAndStatus(@Param("clientId") UUID clientId, @Param("status") ProjectStatus status);

    @Query("SELECT COALESCE(SUM(p.projectBudget), 0) FROM Project p WHERE p.clientId = :clientId")
    BigDecimal sumProjectBudgetByClientId(@Param("clientId") UUID clientId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Project p where p.pmsProjectId = :id")
    Optional<Project> findForUpdate(@Param("id") Long id);


}
