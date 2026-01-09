package com.repo.skill_repo;

import com.entity.skill_entities.ResourceSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResourceSkillRepository extends JpaRepository<ResourceSkill, Long> {

    Optional<ResourceSkill> findByResourceIdAndSkillId(Long resourceId, Long skillId);

    List<ResourceSkill> findByResourceIdAndActiveFlagTrue(Long resourceId);
}
