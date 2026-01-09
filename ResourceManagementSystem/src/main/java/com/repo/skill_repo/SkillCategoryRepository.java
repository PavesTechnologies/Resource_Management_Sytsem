package com.repo.skill_repo;

import com.entity.skill_entities.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillCategoryRepository extends JpaRepository<SkillCategory, Long> {

    Optional<SkillCategory> findByCategoryNameIgnoreCase(String categoryName);

    boolean existsByCategoryNameIgnoreCase(String categoryName);
}
