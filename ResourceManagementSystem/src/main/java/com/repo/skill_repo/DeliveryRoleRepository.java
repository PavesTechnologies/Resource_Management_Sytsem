package com.repo.skill_repo;

import com.entity.skill_entities.DeliveryRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRoleRepository extends JpaRepository<DeliveryRole, Long> {

    Optional<DeliveryRole> findByRoleNameIgnoreCase(String roleName);

    boolean existsByRoleNameIgnoreCase(String roleName);
}
