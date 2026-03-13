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

import java.util.UUID;

public interface RoleOffEventRepository extends JpaRepository<RoleOffEvent, UUID> {
    boolean existsByProject_PmsProjectIdAndResource_ResourceId(
            Long projectId,
            Long resourceId
    );
}
