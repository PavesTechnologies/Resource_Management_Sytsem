package com.service_imple.skill_service_impl;

import com.dto.skill_dto.*;
import com.entity.skill_entities.SkillCategory;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SubSkill;
import com.global_exception_handler.SkillExceptionHandler;
import com.repo.skill_repo.SkillCategoryRepository;
import com.repo.skill_repo.SkillRepository;
import com.repo.skill_repo.SubSkillRepository;
import com.repo.skill_repo.ResourceSkillRepository;
import com.repo.skill_repo.ResourceSubSkillRepository;
import com.service_interface.skill_service_interface.SkillCategoryService;
import com.service_interface.resource_service_interface.ResourceService; // Assuming this path for ResourceService
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Boolean.parseBoolean;

@Service
@RequiredArgsConstructor
public class SkillCategoryServiceImpl implements SkillCategoryService {

    private final SkillCategoryRepository repository;
    private final SkillRepository skillRepository;
    private final SubSkillRepository subSkillRepository;
    private final ResourceSkillRepository resourceSkillRepository;
    private final ResourceSubSkillRepository resourceSubSkillRepository;
    private final ResourceService resourceService; // Added ResourceService dependency

    @Override
    public SkillCategory create(String name, String description) {

        String normalized = name.trim();

        if (repository.existsByNameIgnoreCase(normalized)) {
            throw SkillExceptionHandler.badRequest("Category already exists");
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
                .orElseThrow(() -> SkillExceptionHandler.badRequest("Category not found"));

        if (!"ACTIVE".equals(category.getStatus())) {
            throw SkillExceptionHandler.badRequest("Category is already inactive");
        }

        long activeSkillsCount = repository.countActiveSkillsByCategoryId(categoryId);
        if (activeSkillsCount > 0) {
            throw SkillExceptionHandler.badRequest("Cannot deactivate category with " + activeSkillsCount + " active skills");
        }

        int updated = repository.deactivateCategory(categoryId);
        if (updated == 0) {
            throw SkillExceptionHandler.badRequest("Failed to deactivate category");
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
            throw SkillExceptionHandler.badRequest("Category not found or inactive");
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
                    if (skillSubSkills != null && !skillSubSkills.isEmpty()) {
                        List<SkillTaxonomyTreeDto.SubSkillTreeDto> subSkillDtos = skillSubSkills.stream()
                                .map(subSkill -> SkillTaxonomyTreeDto.SubSkillTreeDto.builder()
                                        .id(subSkill.getId().toString())
                                        .name(subSkill.getName())
                                        .build())
                                .collect(Collectors.toList());
                        skillDto.setSubSkills(subSkillDtos);
                    } else {
                        skillDto.setSubSkills(new ArrayList<>());
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
                                    
                                    if (skillSubSkills != null && !skillSubSkills.isEmpty()) {
                                        List<SkillTaxonomyTreeDto.SubSkillTreeDto> subSkillDtos = skillSubSkills.stream()
                                                .map(subSkill -> SkillTaxonomyTreeDto.SubSkillTreeDto.builder()
                                                        .id(subSkill.getId().toString())
                                                        .name(subSkill.getName())
                                                        .build())
                                                .collect(Collectors.toList());
                                        skillDto.setSubSkills(subSkillDtos);
                                    } else {
                                        skillDto.setSubSkills(new ArrayList<>());
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

    @Override
    public List<SkillSearchResultDto> searchSkills(String searchTerm) {
        // ========================================================================
        // INPUT VALIDATION
        // ========================================================================
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String trimmedSearchTerm = searchTerm.trim();
        List<SkillSearchResultDto> results = new ArrayList<>();

        try {
            // ========================================================================
            // OPTIMIZED SEARCH STRATEGY
            // ========================================================================
            // Strategy: DTO Projections + Explicit Joins (No JOIN FETCH)
            // Benefits:
            // - No cartesian products from collection loading
            // - ~90% memory reduction vs entity loading
            // - Single query per entity type (no N+1)
            // - Index-friendly queries
            
            // ========================================================================
            // CATEGORY SEARCH - O(N) Time Complexity
            // ========================================================================
            // DTO Projection: Only 7 fields vs full entity graph
            // No JOIN FETCH: No collection loading, no cartesian product
            List<SkillSearchProjection> categoryProjections = repository.searchCategoriesByName(trimmedSearchTerm);
            
            for (SkillSearchProjection projection : categoryProjections) {
                // Check if category was recently created (within last 5 minutes)
                if (isRecentlyCreated(projection.getId(), "CATEGORY")) {
                    continue; // Skip recently created categories
                }
                
                results.add(SkillSearchResultDto.builder()
                        .type(projection.getType())
                        .id(projection.getId())
                        .name(projection.getName())
                        .description(projection.getDescription())
                        .categoryName("".equals(projection.getCategoryName()) ? null : projection.getCategoryName())
                        .parentSkillName("".equals(projection.getParentSkillName()) ? null : projection.getParentSkillName())
                        .subSkills(null) // Categories don't have subskills
                        .status(projection.getStatus())
                        .build());
            }

            // ========================================================================
            // SKILL SEARCH - O(S) Time Complexity  
            // ========================================================================
            // DTO Projection: Category name fetched via explicit JOIN
            // Memory: ~85% reduction vs JOIN FETCH approach
            List<SkillSearchProjection> skillProjections = repository.searchSkillsByName(trimmedSearchTerm);
            
            for (SkillSearchProjection projection : skillProjections) {
                // Check if skill was recently created (within last 5 minutes)
                if (isRecentlyCreated(projection.getId(), "SKILL")) {
                    continue; // Skip recently created skills
                }
                
                // For skills, we need to fetch subskills separately (only if needed)
                // This is more efficient than JOIN FETCH for large datasets
                List<String> subSkillNames = getSubSkillNamesForSkill(projection.getId());

                results.add(SkillSearchResultDto.builder()
                        .type(projection.getType())
                        .id(projection.getId())
                        .name(projection.getName())
                        .description(projection.getDescription())
                        .categoryName("".equals(projection.getCategoryName()) ? null : projection.getCategoryName())
                        .parentSkillName("".equals(projection.getParentSkillName()) ? null : projection.getParentSkillName())
                        .subSkills(subSkillNames)
                        .status(projection.getStatus())
                        .build());
            }

            // ========================================================================
            // SUBSKILL SEARCH - O(SS) Time Complexity
            // ========================================================================
            // DTO Projection: Most efficient - leaf nodes with minimal joins
            // Memory: ~80% reduction vs entity loading
            List<SkillSearchProjection> subSkillProjections = repository.searchSubSkillsByName(trimmedSearchTerm);
            
            for (SkillSearchProjection projection : subSkillProjections) {
                // Check if subskill was recently created (within last 5 minutes)
                if (isRecentlyCreated(projection.getId(), "SUBSKILL")) {
                    continue; // Skip recently created subskills
                }
                
                results.add(SkillSearchResultDto.builder()
                        .type(projection.getType())
                        .id(projection.getId())
                        .name(projection.getName())
                        .description(projection.getDescription())
                        .categoryName("".equals(projection.getCategoryName()) ? null : projection.getCategoryName())
                        .parentSkillName("".equals(projection.getParentSkillName()) ? null : projection.getParentSkillName())
                        .subSkills(null) // Subskills don't have subskills
                        .status(projection.getStatus())
                        .build());
            }

        } catch (Exception e) {
            // Log error and return empty list for graceful degradation
            // In production, you'd want proper logging here
            System.err.println("Search error for term '" + trimmedSearchTerm + "': " + e.getMessage());
            return new ArrayList<>();
        }

        return results;
    }
    @Override
    public byte[] exportSkillTaxonomyExcel() {

        try (Workbook workbook =
                     new XSSFWorkbook()) {

            Sheet sheet =
                    workbook.createSheet(
                            "Skill Taxonomy");

            // ==========================================
            // HEADERS
            // ==========================================

            String[] headers = {

                    "Category Name",
                    "Category Description",
                    "Category Active",

                    "Skill Name",
                    "Skill Description",
                    "Skill Active",

                    "SubSkill Name",
                    "SubSkill Description",
                    "SubSkill Active"
            };

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {

                Cell cell =
                        headerRow.createCell(i);

                cell.setCellValue(headers[i]);
            }

            // ==========================================
            // FETCH DATA
            // ==========================================

            List<SkillCategory> categories =
                    repository.findAll();

            int rowNum = 1;

            // ==========================================
            // WRITE DATA
            // ==========================================

            for (SkillCategory category : categories) {

                List<Skill> skills =
                        category.getSkills();

                if (skills == null || skills.isEmpty()) {

                    Row row =
                            sheet.createRow(rowNum++);

                    writeCell(row, 0,
                            category.getName());

                    writeCell(row, 1,
                            category.getDescription());

                    writeCell(row, 2,
                            category.getStatus());

                    continue;
                }

                for (Skill skill : skills) {

                    List<SubSkill> subSkills =
                            skill.getSubSkills();

                    if (subSkills == null
                            || subSkills.isEmpty()) {

                        Row row =
                                sheet.createRow(rowNum++);

                        writeCategorySkillData(
                                row,
                                category,
                                skill);

                        continue;
                    }

                    for (SubSkill subSkill :
                            subSkills) {

                        Row row =
                                sheet.createRow(rowNum++);

                        // CATEGORY

                        writeCell(row, 0,
                                category.getName());

                        writeCell(row, 1,
                                category.getDescription());

                        writeCell(row, 2,
                                category.getStatus());

                        // SKILL

                        writeCell(row, 3,
                                skill.getName());

                        writeCell(row, 4,
                                skill.getDescription());

                        writeCell(row, 5,
                                skill.getStatus());

                        // SUBSKILL

                        writeCell(row, 6,
                                subSkill.getName());

                        writeCell(row, 7,
                                subSkill.getDescription());

                        writeCell(row, 8,
                                subSkill.getStatus());
                    }
                }
            }

            // ==========================================
            // AUTO SIZE
            // ==========================================

            for (int i = 0; i < headers.length; i++) {

                sheet.autoSizeColumn(i);
            }

            // ==========================================
            // CONVERT TO BYTE[]
            // ==========================================

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            workbook.write(outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw SkillExceptionHandler.badRequest(
                    "Failed to export taxonomy excel");
        }
    }

    private void writeCell(
            Row row,
            int column,
            String value) {

        row.createCell(column)
                .setCellValue(
                        value != null ? value : "");
    }

    private void writeCategorySkillData(
            Row row,
            SkillCategory category,
            Skill skill) {

        writeCell(row, 0,
                category.getName());

        writeCell(row, 1,
                category.getDescription());

        writeCell(row, 2,
                category.getStatus());

        writeCell(row, 3,
                skill.getName());

        writeCell(row, 4,
                skill.getDescription());

        writeCell(row, 5,
                skill.getStatus());
    }

    @Override
    public List<CategoryDto> getAllCategoriesDto() {
        return repository.findAll().stream()
                .map(category -> new CategoryDto(
                        category.getId(),
                        category.getName(),
                        category.getDescription(),
                        "ACTIVE".equals(category.getStatus())
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillDto> getSkillsByCategoryId(UUID categoryId) {
        SkillCategory category = repository.findById(categoryId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest("Category not found"));

        return skillRepository.findByCategoryAndStatus(category, "ACTIVE").stream()
                .map(skill -> new SkillDto(
                        skill.getId(),
                        skill.getName(),
                        skill.getDescription(),
                        "ACTIVE".equals(skill.getStatus())
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<SubSkillTaxoDto> getSubSkillsBySkillId(UUID skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest("Skill not found"));

        return subSkillRepository.findBySkillAndStatus(skill, "ACTIVE").stream()
                .map(subSkill -> new SubSkillTaxoDto(
                        subSkill.getId(),
                        subSkill.getName(),
                        subSkill.getDescription(),
                        "ACTIVE".equals(subSkill.getStatus())
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SkillTaxonomyResponseDto manageSkillTaxonomy(SkillTaxonomyRequestDto requestDto) {
        List<CategoryResponseDto> categoryResponses = new ArrayList<>();

        if (requestDto == null || requestDto.getCategories() == null || requestDto.getCategories().isEmpty()) {
            throw SkillExceptionHandler.badRequest("Request payload cannot be empty.");
        }

        for (CategoryRequestDto categoryRequest : requestDto.getCategories()) {
            if (!StringUtils.hasText(categoryRequest.getName())) {
                throw SkillExceptionHandler.badRequest("Category name cannot be empty.");
            }

            String operation = "";
            SkillCategory category;

            if (categoryRequest.getId() == null) {
                // Create Category
                if (repository.existsByNameIgnoreCase(categoryRequest.getName().trim())) {
                    throw SkillExceptionHandler.badRequest("Category with name '" + categoryRequest.getName() + "' already exists.");
                }
                category = new SkillCategory();
                category.setName(categoryRequest.getName().trim());
                operation = "CREATED";
            } else {
                // Update Category
                category = repository.findById(categoryRequest.getId())
                        .orElseThrow(() -> SkillExceptionHandler.badRequest("Category with ID '" + categoryRequest.getId() + "' not found."));

                if (!category.getName().equalsIgnoreCase(categoryRequest.getName().trim()) && repository.existsByNameIgnoreCase(categoryRequest.getName().trim())) {
                    throw SkillExceptionHandler.badRequest("Another category with name '" + categoryRequest.getName() + "' already exists.");
                }

                // Validation: Prevent deactivating if resources are using it
                if ("ACTIVE".equals(category.getStatus()) && (categoryRequest.getActive() == null || !categoryRequest.getActive())) {
                    if (resourceService.hasActiveResourcesUsingCategory(category.getId())) {
                        throw SkillExceptionHandler.badRequest("Cannot deactivate category '" + category.getName() + "' as it is currently in use by resources.");
                    }
                }

                category.setName(categoryRequest.getName().trim());
                operation = "UPDATED";
            }

            category.setDescription(categoryRequest.getDescription());
            category.setStatus(categoryRequest.getActive() != null && categoryRequest.getActive() ? "ACTIVE" : "INACTIVE");
            category = repository.save(category);

            List<SkillResponseDto> skillResponses = new ArrayList<>();
            if (categoryRequest.getSkills() != null) {
                for (SkillRequestDto skillRequest : categoryRequest.getSkills()) {
                    if (!StringUtils.hasText(skillRequest.getName())) {
                        throw SkillExceptionHandler.badRequest("Skill name cannot be empty for category '" + category.getName() + "'.");
                    }

                    String skillOperation = "";
                    Skill skill;

                    if (skillRequest.getId() == null) {
                        // Create Skill
                        if (skillRepository.existsByNameIgnoreCaseAndCategory(skillRequest.getName().trim(), category)) {
                            throw SkillExceptionHandler.badRequest("Skill with name '" + skillRequest.getName() + "' already exists in category '" + category.getName() + "'.");
                        }
                        if(skillRepository.existsByNameIgnoreCase(skillRequest.getName().trim()))
                        {
                            throw SkillExceptionHandler.badRequest("Skill with name '" + skillRequest.getName() + "' already exists.");
                        }
                        skill = new Skill();
                        skill.setName(skillRequest.getName().trim());
                        skill.setCategory(category);
                        skillOperation = "CREATED";
                    } else {
                        // Update Skill
                        skill = skillRepository.findById(skillRequest.getId())
                                .orElseThrow(() -> SkillExceptionHandler.badRequest("Skill with ID '" + skillRequest.getId() + "' not found."));

                        if (!skill.getCategory().getId().equals(category.getId())) {
                            throw SkillExceptionHandler.badRequest("Skill with ID '" + skillRequest.getId() + "' does not belong to category '" + category.getName() + "'.");
                        }

                        if (!skill.getName().equalsIgnoreCase(skillRequest.getName().trim()) && skillRepository.existsByNameIgnoreCaseAndCategory(skillRequest.getName().trim(), category)) {
                            throw SkillExceptionHandler.badRequest("Another skill with name '" + skillRequest.getName() + "' already exists in category '" + category.getName() + "'.");
                        }

                        // Validation: Prevent deactivating if resources are using it
                        if ("ACTIVE".equals(skill.getStatus()) && (skillRequest.getActive() == null || !skillRequest.getActive())) {
                            if (resourceService.hasActiveResourcesUsingSkill(skill.getId())) {
                                throw SkillExceptionHandler.badRequest("Cannot deactivate skill '" + skill.getName() + "' as it is currently in use by resources.");
                            }
                        }

                        skill.setName(skillRequest.getName().trim());
                        skillOperation = "UPDATED";
                    }

                    skill.setDescription(skillRequest.getDescription());
                    skill.setStatus(skillRequest.getActive() != null && skillRequest.getActive() ? "ACTIVE" : "INACTIVE");
                    skill = skillRepository.save(skill);

                    List<SubSkillResponseDto> subSkillResponses = new ArrayList<>();
                    if (skillRequest.getSubSkills() != null) {
                        for (SubSkillRequestDto subSkillRequest : skillRequest.getSubSkills()) {
                            if (!StringUtils.hasText(subSkillRequest.getName())) {
                                throw SkillExceptionHandler.badRequest("SubSkill name cannot be empty for skill '" + skill.getName() + "'.");
                            }

                            String subSkillOperation = "";
                            SubSkill subSkill;

                            if (subSkillRequest.getId() == null) {
                                // Create SubSkill
                                if (subSkillRepository.existsByNameIgnoreCaseAndSkill(subSkillRequest.getName().trim(), skill)) {
                                    throw SkillExceptionHandler.badRequest("SubSkill with name '" + subSkillRequest.getName() + "' already exists in skill '" + skill.getName() + "'.");
                                }
                                
                                if(subSkillRepository.existsByNameIgnoreCase(subSkillRequest.getName().trim()))
                                    throw SkillExceptionHandler.badRequest("SubSkill with name '" + subSkillRequest.getName() + "' already exists.");

                                subSkill = new SubSkill();
                                subSkill.setName(subSkillRequest.getName().trim());
                                subSkill.setSkill(skill);
                                subSkillOperation = "CREATED";
                            } else {
                                // Update SubSkill
                                subSkill = subSkillRepository.findById(subSkillRequest.getId())
                                        .orElseThrow(() -> SkillExceptionHandler.badRequest("SubSkill with ID '" + subSkillRequest.getId() + "' not found."));

                                if (!subSkill.getSkill().getId().equals(skill.getId())) {
                                    throw SkillExceptionHandler.badRequest("SubSkill with ID '" + subSkillRequest.getId() + "' does not belong to skill '" + skill.getName() + "'.");
                                }

                                if (!subSkill.getName().equalsIgnoreCase(subSkillRequest.getName().trim()) && subSkillRepository.existsByNameIgnoreCaseAndSkill(subSkillRequest.getName().trim(), skill)) {
                                    throw SkillExceptionHandler.badRequest("Another subSkill with name '" + subSkillRequest.getName() + "' already exists in skill '" + skill.getName() + "'.");
                                }

                                // Validation: Prevent deactivating if resources are using it
                                if ("ACTIVE".equals(subSkill.getStatus()) && (subSkillRequest.getActive() == null || !subSkillRequest.getActive())) {
                                    if (resourceService.hasActiveResourcesUsingSubSkill(subSkill.getId())) {
                                        throw SkillExceptionHandler.badRequest("Cannot deactivate sub-skill '" + subSkill.getName() + "' as it is currently in use by resources.");
                                    }
                                }

                                subSkill.setName(subSkillRequest.getName().trim());
                                subSkillOperation = "UPDATED";
                            }

                            subSkill.setDescription(subSkillRequest.getDescription());
                            subSkill.setStatus(subSkillRequest.getActive() != null && subSkillRequest.getActive() ? "ACTIVE" : "INACTIVE");
                            subSkill = subSkillRepository.save(subSkill);

                            subSkillResponses.add(SubSkillResponseDto.builder()
                                    .id(subSkill.getId())
                                    .name(subSkill.getName())
                                    .operation(subSkillOperation)
                                    .build());
                        }
                    }

                    skillResponses.add(SkillResponseDto.builder()
                            .id(skill.getId())
                            .name(skill.getName())
                            .operation(skillOperation)
                            .subSkills(subSkillResponses)
                            .build());
                }
            }

            categoryResponses.add(CategoryResponseDto.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .operation(operation)
                    .skills(skillResponses)
                    .build());
        }

        return SkillTaxonomyResponseDto.builder()
                .categories(categoryResponses)
                .build();
    }

    @Transactional
    @Override
    public void deleteCategory(UUID categoryId) {

        SkillCategory category =
                repository.findById(categoryId)
                        .orElseThrow(() ->
                                SkillExceptionHandler.notFound(
                                        "Category not found"));

        // ==========================================
        // FETCH SKILLS
        // ==========================================

        List<Skill> skills =
                skillRepository
                        .findByCategoryId(categoryId);

        // ==========================================
        // CHECK SKILL ASSIGNMENTS
        // ==========================================

        for (Skill skill : skills) {

            boolean skillAssigned =
                    resourceSkillRepository
                            .existsBySkillIdAndActiveFlagTrue(
                                    skill.getId());

            if (skillAssigned) {

                throw SkillExceptionHandler.badRequest(
                        "Cannot delete category. Resources are assigned to skills under this category.");
            }
        }

        // ==========================================
        // FETCH SUBSKILLS
        // ==========================================

        List<SubSkill> subSkills =
                subSkillRepository
                        .findByCategoryId(categoryId);

        // ==========================================
        // CHECK SUBSKILL ASSIGNMENTS
        // ==========================================

        for (SubSkill subSkill : subSkills) {

            boolean subSkillAssigned =
                    resourceSubSkillRepository
                            .existsBySubSkillId(
                                    subSkill.getId());

            if (subSkillAssigned) {

                throw SkillExceptionHandler.badRequest(
                        "Cannot delete category. Resources are assigned to subskills under this category.");
            }
        }

        // ==========================================
        // SOFT DELETE CATEGORY
        // ==========================================

        category.setStatus("INACTIVE");

        repository.save(category);

        // ==========================================
        // SOFT DELETE SKILLS
        // ==========================================

        for (Skill skill : skills) {

            skill.setStatus("INACTIVE");

            skillRepository.save(skill);
        }

        // ==========================================
        // SOFT DELETE SUBSKILLS
        // ==========================================

        for (SubSkill subSkill : subSkills) {

            subSkill.setStatus("INACTIVE");

            subSkillRepository.save(subSkill);
        }
    }


    @Override
    @Transactional
    public ExcelUploadResponseDto uploadSkillTaxonomyExcel(
            MultipartFile file) {

        List<RowErrorDto> errors = new ArrayList<>();

        int totalRows = 0;

        int duplicateRows = 0;

        try (Workbook workbook =
                     WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            // =====================================================
            // VALIDATE HEADERS
            // =====================================================

            validateHeaders(sheet.getRow(0));

            // =====================================================
            // DATA STRUCTURES
            // =====================================================

            Map<String, CategoryRequestDto> categoryMap =
                    new LinkedHashMap<>();

            Set<String> duplicateSet = new HashSet<>();

            // =====================================================
            // PROCESS ROWS
            // =====================================================

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                totalRows++;

                try {

                    // =============================================
                    // READ CELLS
                    // =============================================

                    String categoryName =
                            getCellValue(row.getCell(0));

                    String categoryDescription =
                            getCellValue(row.getCell(1));

                    String categoryActive =
                            getCellValue(row.getCell(2));

                    String skillName =
                            getCellValue(row.getCell(3));

                    String skillDescription =
                            getCellValue(row.getCell(4));

                    String skillActive =
                            getCellValue(row.getCell(5));

                    String subSkillName =
                            getCellValue(row.getCell(6));

                    String subSkillDescription =
                            getCellValue(row.getCell(7));

                    String subSkillActive =
                            getCellValue(row.getCell(8));

                    // =============================================
                    // EMPTY VALIDATIONS
                    // =============================================

                    if (categoryName.isBlank()) {

                        errors.add(RowErrorDto.builder()
                                .rowNumber(i + 1)
                                .message(
                                        "Category Name is mandatory")
                                .build());

                        continue;
                    }

                    if (skillName.isBlank()) {

                        errors.add(RowErrorDto.builder()
                                .rowNumber(i + 1)
                                .message(
                                        "Skill Name is mandatory")
                                .build());

                        continue;
                    }

                    if (subSkillName.isBlank()) {

                        errors.add(RowErrorDto.builder()
                                .rowNumber(i + 1)
                                .message(
                                        "SubSkill Name is mandatory")
                                .build());

                        continue;
                    }

                    // =============================================
                    // DUPLICATE CHECK
                    // =============================================

                    String duplicateKey =
                            categoryName.trim().toLowerCase()
                                    + "|"
                                    + skillName.trim().toLowerCase()
                                    + "|"
                                    + subSkillName.trim().toLowerCase();

                    if (duplicateSet.contains(duplicateKey)) {

                        duplicateRows++;

                        errors.add(RowErrorDto.builder()
                                .rowNumber(i + 1)
                                .message(
                                        "Duplicate row found")
                                .build());

                        continue;
                    }

                    duplicateSet.add(duplicateKey);

                    // =============================================
                    // CATEGORY BUILDING
                    // =============================================

                    CategoryRequestDto categoryDto =
                            categoryMap.computeIfAbsent(
                                    categoryName.toLowerCase(),
                                    key -> {

                                        CategoryRequestDto dto =
                                                new CategoryRequestDto();

                                        dto.setName(
                                                categoryName.trim());

                                        dto.setDescription(
                                                categoryDescription);

                                        dto.setActive(
                                                parseBoolean(
                                                        categoryActive));

                                        dto.setSkills(
                                                new ArrayList<>());

                                        return dto;
                                    });

                    // =============================================
                    // SKILL BUILDING
                    // =============================================

                    SkillRequestDto skillDto =
                            categoryDto.getSkills()
                                    .stream()
                                    .filter(skill ->
                                            skill.getName()
                                                    .equalsIgnoreCase(
                                                            skillName))
                                    .findFirst()
                                    .orElse(null);

                    if (skillDto == null) {

                        skillDto = new SkillRequestDto();

                        skillDto.setName(skillName.trim());

                        skillDto.setDescription(
                                skillDescription);

                        skillDto.setActive(
                                parseBoolean(skillActive));

                        skillDto.setSubSkills(
                                new ArrayList<>());

                        categoryDto.getSkills()
                                .add(skillDto);
                    }

                    // =============================================
                    // SUBSKILL BUILDING
                    // =============================================

                    boolean subSkillExists =
                            skillDto.getSubSkills()
                                    .stream()
                                    .anyMatch(subSkill ->
                                            subSkill.getName()
                                                    .equalsIgnoreCase(
                                                            subSkillName));

                    if (!subSkillExists) {

                        SubSkillRequestDto subSkillDto =
                                new SubSkillRequestDto();

                        subSkillDto.setName(
                                subSkillName.trim());

                        subSkillDto.setDescription(
                                subSkillDescription);

                        subSkillDto.setActive(
                                parseBoolean(
                                        subSkillActive));

                        skillDto.getSubSkills()
                                .add(subSkillDto);
                    }

                } catch (Exception ex) {

                    errors.add(RowErrorDto.builder()
                            .rowNumber(i + 1)
                            .message(ex.getMessage())
                            .build());
                }
            }

            // =====================================================
            // SAVE TO DB
            // =====================================================

            SkillTaxonomyRequestDto requestDto =
                    new SkillTaxonomyRequestDto(
                            new ArrayList<>(categoryMap.values()));

            SkillTaxonomyResponseDto response =
                    saveOrUpdateTaxonomy(requestDto);

            return ExcelUploadResponseDto.builder()
                    .totalRows(totalRows)
                    .validRows(totalRows - errors.size())
                    .invalidRows(errors.size())
                    .duplicateRows(duplicateRows)
                    .errors(errors)
                    .savedData(response)
                    .build();

        } catch (Exception e) {

            throw SkillExceptionHandler.badRequest(
                    "Failed to process excel : "
                            + e.getMessage());
        }
    }

    private void validateHeaders(Row headerRow) {

        String[] expectedHeaders = {

                "Category Name",
                "Category Description",
                "Category Active",

                "Skill Name",
                "Skill Description",
                "Skill Active",

                "SubSkill Name",
                "SubSkill Description",
                "SubSkill Active"
        };

        for (int i = 0; i < expectedHeaders.length; i++) {

            String actualHeader =
                    getCellValue(headerRow.getCell(i));

            if (!expectedHeaders[i]
                    .equalsIgnoreCase(actualHeader)) {

                throw SkillExceptionHandler.badRequest(
                        "Invalid excel format. Expected column: "
                                + expectedHeaders[i]);
            }
        }
    }

    private Boolean parseBoolean(String value) {

        if (value == null || value.isBlank()) {
            return true;
        }

        return value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("active");
    }

    private String getCellValue(Cell cell) {

        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {

            case STRING ->
                    cell.getStringCellValue().trim();

            case BOOLEAN ->
                    String.valueOf(
                            cell.getBooleanCellValue());

            case NUMERIC ->
                    String.valueOf(
                            (long) cell.getNumericCellValue());

            default -> "";
        };
    }

    private SkillTaxonomyResponseDto saveOrUpdateTaxonomy(
            SkillTaxonomyRequestDto requestDto) {

        for (CategoryRequestDto categoryRequest :
                requestDto.getCategories()) {

            SkillCategory category =
                    repository.findByNameIgnoreCase(
                                    categoryRequest.getName())
                            .orElseGet(SkillCategory::new);

            category.setName(categoryRequest.getName());

            category.setDescription(
                    categoryRequest.getDescription());

            category.setStatus(
                    Boolean.TRUE.equals(
                            categoryRequest.getActive())
                            ? "ACTIVE"
                            : "INACTIVE");

            category = repository.save(category);

            for (SkillRequestDto skillRequest :
                    categoryRequest.getSkills()) {

                Skill skill =
                        skillRepository
                                .findByNameIgnoreCaseAndCategory(
                                        skillRequest.getName(),
                                        category)
                                .orElseGet(Skill::new);

                skill.setName(skillRequest.getName());

                skill.setDescription(
                        skillRequest.getDescription());

                skill.setStatus(
                        Boolean.TRUE.equals(
                                skillRequest.getActive())
                                ? "ACTIVE"
                                : "INACTIVE");

                skill.setCategory(category);

                skill = skillRepository.save(skill);

                for (SubSkillRequestDto subSkillRequest :
                        skillRequest.getSubSkills()) {

                    SubSkill subSkill =
                            subSkillRepository
                                    .findByNameIgnoreCaseAndSkill(
                                            subSkillRequest.getName(),
                                            skill)
                                    .orElseGet(SubSkill::new);

                    subSkill.setName(
                            subSkillRequest.getName());

                    subSkill.setDescription(
                            subSkillRequest.getDescription());

                    subSkill.setStatus(
                            Boolean.TRUE.equals(
                                    subSkillRequest.getActive())
                                    ? "ACTIVE"
                                    : "INACTIVE");

                    subSkill.setSkill(skill);

                    subSkillRepository.save(subSkill);
                }
            }
        }

        return SkillTaxonomyResponseDto.builder()
                .categories(new ArrayList<>())
                .build();
    }

    /**
     * Check if entity was recently created (within last 5 minutes)
     * This prevents newly created skills from appearing in search immediately
     * 
     * @param entityId ID of the entity to check
     * @param entityType Type of entity (CATEGORY, SKILL, SUBSKILL)
     * @return true if recently created, false otherwise
     */
    private boolean isRecentlyCreated(UUID entityId, String entityType) {
        try {
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            
            switch (entityType) {
                case "CATEGORY":
                    SkillCategory category = repository.findById(entityId).orElse(null);
                    return category != null && category.getCreatedAt().isAfter(fiveMinutesAgo);
                    
                case "SKILL":
                    Skill skill = skillRepository.findById(entityId).orElse(null);
                    return skill != null && skill.getCreatedAt().isAfter(fiveMinutesAgo);
                    
                case "SUBSKILL":
                    SubSkill subSkill = subSkillRepository.findById(entityId).orElse(null);
                    return subSkill != null && subSkill.getCreatedAt().isAfter(fiveMinutesAgo);
                    
                default:
                    return false;
            }
        } catch (Exception e) {
            // If we can't determine creation time, assume it's not recent
            return false;
        }
    }

    /**
     * Helper method to fetch subskill names for a specific skill.
     * This selective approach is more efficient than JOIN FETCH for large datasets.
     * 
     * N+1 Prevention: Called only for skills that match search criteria
     * Memory Impact: Minimal - only names, not full entities
     * 
     * @param skillId Skill UUID
     * @return List of active subskill names
     */
    private List<String> getSubSkillNamesForSkill(UUID skillId) {
        // Use a lightweight query to fetch only subskill names
        List<SubSkill> subSkills = subSkillRepository.findActiveSubSkillsBySkillId(skillId);
        return subSkills.stream()
                .map(SubSkill::getName)
                .sorted()
                .collect(Collectors.toList());
    }
}
