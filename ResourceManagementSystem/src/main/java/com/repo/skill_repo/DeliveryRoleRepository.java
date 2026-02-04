package com.repo.skill_repo;

import com.entity.skill_entities.DeliveryRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRoleRepository extends JpaRepository<DeliveryRole, UUID> {

    Optional<DeliveryRole> findByRoleNameIgnoreCase(String roleName);

    boolean existsByRoleNameIgnoreCase(String roleName);
}
