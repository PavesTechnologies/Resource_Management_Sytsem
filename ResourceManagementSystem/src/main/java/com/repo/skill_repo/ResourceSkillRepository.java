package com.repo.skill_repo;

import com.entity.skill_entities.ResourceSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceSkillRepository extends JpaRepository<ResourceSkill, UUID> {

    Optional<ResourceSkill> findByResourceIdAndSkillId(Long resourceId, UUID skillId);

    List<ResourceSkill> findByResourceIdAndActiveFlagTrue(Long resourceId);
}
