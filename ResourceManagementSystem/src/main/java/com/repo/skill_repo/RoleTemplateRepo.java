package com.repo.skill_repo;

import com.entity.skill_entities.RoleTemplate;
import com.entity_enums.skill_enums.TemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleTemplateRepo extends JpaRepository<RoleTemplate, Long> {

    Optional<RoleTemplate> findByRoleNameAndStatus(String roleName, TemplateStatus status);

    Optional<RoleTemplate> findByRoleTemplateIdAndStatus(Long id, TemplateStatus status);

    List<RoleTemplate> findByStatus(TemplateStatus status);

    boolean existsByRoleNameAndVersion(String roleName, Integer version);
}

