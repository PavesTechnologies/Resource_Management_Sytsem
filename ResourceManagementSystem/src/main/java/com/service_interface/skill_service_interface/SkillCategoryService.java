package com.service_interface.skill_service_interface;

import com.entity.skill_entities.SkillCategory;
import com.dto.UserDTO;

import java.util.List;
import java.util.Optional;

public interface SkillCategoryService {

    SkillCategory createCategory(String categoryName, UserDTO user);

    SkillCategory updateCategory(Long categoryId, String categoryName, UserDTO user);

    void deactivateCategory(Long categoryId, UserDTO user);

    Optional<SkillCategory> getActiveCategory(Long categoryId);

    List<SkillCategory> getAllActiveCategories();
}
