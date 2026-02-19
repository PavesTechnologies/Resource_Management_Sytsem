package com.repo.skill_repo;

import com.entity.skill_entities.DemandRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemandRoleRepo extends JpaRepository<DemandRole, Long> {

    long countBySourceTemplateId(Long templateId);
}

