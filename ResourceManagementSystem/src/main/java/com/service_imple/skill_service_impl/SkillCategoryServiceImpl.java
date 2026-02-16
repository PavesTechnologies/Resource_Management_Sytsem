package com.service_imple.skill_service_impl;

import com.dto.skill_taxonomy.SkillTaxonomyTreeDto;
import com.entity.skill_entities.SkillCategory;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SubSkill;
import com.global_exception_handler.SkillTaxonomyExceptionHandler;
import com.repo.skill_repo.SkillCategoryRepository;
import com.repo.skill_repo.SkillRepository;
import com.repo.skill_repo.SubSkillRepository;
import com.service_interface.skill_service_interface.SkillCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillCategoryServiceImpl implements SkillCategoryService {

    private final SkillCategoryRepository repository;
    private final SkillRepository skillRepository;
    private final SubSkillRepository subSkillRepository;

    @Override
    public SkillCategory create(String name, String description) {

        String normalized = name.trim();

        if (repository.existsByNameIgnoreCase(normalized)) {
            throw new SkillTaxonomyExceptionHandler("Category already exists");
        }

        SkillCategory category = new SkillCategory();
        category.setName(normalized);
        category.setDescription(description);

        return repository.save(category);
    }

    @Override
    public List<SkillCategory> findAll() {
        return repository.findAll();
    }

    @Override
    public List<SkillCategory> findActiveCategories() {
        return repository.findActiveCategories();
    }

    @Override
    public void deactivateCategory(UUID categoryId) {
        SkillCategory category = repository.findById(categoryId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler("Category not found"));

        if (!"ACTIVE".equals(category.getStatus())) {
            throw new SkillTaxonomyExceptionHandler("Category is already inactive");
        }

        long activeSkillsCount = repository.countActiveSkillsByCategoryId(categoryId);
        if (activeSkillsCount > 0) {
            throw new SkillTaxonomyExceptionHandler("Cannot deactivate category with " + activeSkillsCount + " active skills");
        }

        int updated = repository.deactivateCategory(categoryId);
        if (updated == 0) {
            throw new SkillTaxonomyExceptionHandler("Failed to deactivate category");
        }
    }

    @Override
    public List<SkillTaxonomyTreeDto> getSkillTaxonomyTree() {
        List<SkillCategory> categories = repository.findSkillTaxonomyTree();
        return buildTaxonomyTree(categories);
    }

    @Override
    public SkillTaxonomyTreeDto getSkillTaxonomyTreeByCategoryId(UUID categoryId) {
        SkillCategory category = repository.findActiveCategoryById(categoryId);
        if (category == null) {
            throw new SkillTaxonomyExceptionHandler("Category not found or inactive");
        }

        List<Skill> skills = repository.findActiveSkillsByCategoryId(categoryId);
        List<UUID> skillIds = skills.stream().map(Skill::getId).collect(Collectors.toList());
        List<SubSkill> subSkills = subSkillRepository.findActiveSubSkillsBySkillIds(skillIds);

        Map<UUID, List<SubSkill>> subSkillsBySkillId = subSkills.stream()
                .collect(Collectors.groupingBy(subSkill -> subSkill.getSkill().getId()));

        List<SkillTaxonomyTreeDto.SkillTreeDto> skillDtos = skills.stream()
                .map(skill -> {
                    SkillTaxonomyTreeDto.SkillTreeDto skillDto = SkillTaxonomyTreeDto.SkillTreeDto.builder()
                            .id(skill.getId().toString())
                            .name(skill.getName())
                            .build();

                    List<SubSkill> skillSubSkills = subSkillsBySkillId.get(skill.getId());
                    if (skillSubSkills != null) {
                        List<SkillTaxonomyTreeDto.SubSkillTreeDto> subSkillDtos = skillSubSkills.stream()
                                .map(subSkill -> SkillTaxonomyTreeDto.SubSkillTreeDto.builder()
                                        .id(subSkill.getId().toString())
                                        .name(subSkill.getName())
                                        .build())
                                .collect(Collectors.toList());
                        skillDto.setSubSkills(subSkillDtos);
                    }

                    return skillDto;
                })
                .collect(Collectors.toList());

        return SkillTaxonomyTreeDto.builder()
                .id(category.getId().toString())
                .name(category.getName())
                .skills(skillDtos)
                .build();
    }

    private List<SkillTaxonomyTreeDto> buildTaxonomyTree(List<SkillCategory> categories) {
        List<UUID> categoryIds = categories.stream().map(SkillCategory::getId).collect(Collectors.toList());
        List<Skill> allSkills = skillRepository.findActiveSkills();
        List<UUID> skillIds = allSkills.stream().map(Skill::getId).collect(Collectors.toList());
        List<SubSkill> allSubSkills = subSkillRepository.findActiveSubSkillsBySkillIds(skillIds);

        Map<UUID, List<Skill>> skillsByCategoryId = allSkills.stream()
                .collect(Collectors.groupingBy(skill -> skill.getCategory().getId()));

        Map<UUID, List<SubSkill>> subSkillsBySkillId = allSubSkills.stream()
                .collect(Collectors.groupingBy(subSkill -> subSkill.getSkill().getId()));

        return categories.stream()
                .map(category -> {
                    SkillTaxonomyTreeDto categoryDto = SkillTaxonomyTreeDto.builder()
                            .id(category.getId().toString())
                            .name(category.getName())
                            .build();

                    List<Skill> categorySkills = skillsByCategoryId.get(category.getId());
                    if (categorySkills != null) {
                        List<SkillTaxonomyTreeDto.SkillTreeDto> skillDtos = categorySkills.stream()
                                .map(skill -> {
                                    SkillTaxonomyTreeDto.SkillTreeDto skillDto = SkillTaxonomyTreeDto.SkillTreeDto.builder()
                                            .id(skill.getId().toString())
                                            .name(skill.getName())
                                            .build();

                                    List<SubSkill> skillSubSkills = subSkillsBySkillId.get(skill.getId());
                                    if (skillSubSkills != null) {
                                        List<SkillTaxonomyTreeDto.SubSkillTreeDto> subSkillDtos = skillSubSkills.stream()
                                                .map(subSkill -> SkillTaxonomyTreeDto.SubSkillTreeDto.builder()
                                                        .id(subSkill.getId().toString())
                                                        .name(subSkill.getName())
                                                        .build())
                                                .collect(Collectors.toList());
                                        skillDto.setSubSkills(subSkillDtos);
                                    }

                                    return skillDto;
                                })
                                .collect(Collectors.toList());
                        categoryDto.setSkills(skillDtos);
                    }

                    return categoryDto;
                })
                .collect(Collectors.toList());
    }
}

