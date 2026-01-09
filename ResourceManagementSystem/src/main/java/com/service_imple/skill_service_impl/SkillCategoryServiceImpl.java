package com.service_imple.skill_service_impl;

import com.entity.skill_entities.SkillCategory;
import com.repo.skill_repo.SkillCategoryRepository;
import com.service_interface.skill_service_interface.SkillAuditService;
import com.service_interface.skill_service_interface.SkillCategoryService;
import com.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SkillCategoryServiceImpl implements SkillCategoryService {

    private final SkillCategoryRepository categoryRepo;
    private final SkillAuditService auditService;

    @Override
    public SkillCategory createCategory(String categoryName, UserDTO user) {
        categoryRepo.findByCategoryNameIgnoreCase(categoryName)
                .ifPresent(c -> {
                    throw new IllegalStateException("Duplicate skill category");
                });

        SkillCategory category = SkillCategory.builder()
                .categoryName(categoryName)
                .activeFlag(true)
                .build();

        SkillCategory saved = categoryRepo.save(category);

        auditService.auditCreate(
                "SKILL_CATEGORY",
                saved.getCategoryId().toString(),
                saved,
                user.getEmail()
        );

        return saved;
    }

    @Override
    public SkillCategory updateCategory(Long categoryId, String categoryName, UserDTO user) {
        SkillCategory existing = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new IllegalStateException("Category not found"));

        SkillCategory before = SkillCategory.builder()
                .categoryId(existing.getCategoryId())
                .categoryName(existing.getCategoryName())
                .activeFlag(existing.getActiveFlag())
                .build();

        existing.setCategoryName(categoryName);

        SkillCategory saved = categoryRepo.save(existing);

        auditService.auditUpdate(
                "SKILL_CATEGORY",
                saved.getCategoryId().toString(),
                before,
                saved,
                user.getEmail()
        );

        return saved;
    }

    @Override
    public void deactivateCategory(Long categoryId, UserDTO user) {
        SkillCategory existing = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new IllegalStateException("Category not found"));

        if (!existing.getActiveFlag()) {
            return;
        }

        SkillCategory before = SkillCategory.builder()
                .categoryId(existing.getCategoryId())
                .categoryName(existing.getCategoryName())
                .activeFlag(existing.getActiveFlag())
                .build();

        existing.setActiveFlag(false);
        categoryRepo.save(existing);

        auditService.auditUpdate(
                "SKILL_CATEGORY",
                existing.getCategoryId().toString(),
                before,
                existing,
                user.getEmail()
        );
    }

    @Override
    public Optional<SkillCategory> getActiveCategory(Long categoryId) {
        return categoryRepo.findById(categoryId)
                .filter(SkillCategory::getActiveFlag);
    }

    @Override
    public List<SkillCategory> getAllActiveCategories() {
        return categoryRepo.findAll()
                .stream()
                .filter(SkillCategory::getActiveFlag)
                .toList();
    }
}
