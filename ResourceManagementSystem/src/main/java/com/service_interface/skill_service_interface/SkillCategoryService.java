package com.service_interface.skill_service_interface;

import com.entity.skill_entities.SkillCategory;
import com.dto.UserDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillCategoryService {

    SkillCategory createCategory(String categoryName, UserDTO user);

    SkillCategory updateCategory(UUID categoryId, String categoryName, UserDTO user);

    void deactivateCategory(UUID categoryId, UserDTO user);

    Optional<SkillCategory> getActiveCategory(UUID categoryId);

    List<SkillCategory> getAllActiveCategories();
}
