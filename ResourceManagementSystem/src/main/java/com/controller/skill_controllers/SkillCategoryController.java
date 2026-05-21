package com.controller.skill_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.skill_dto.CategoryDto;
import com.dto.skill_dto.SkillDto;
import com.dto.skill_dto.SkillSearchResultDto;
import com.dto.skill_dto.SkillTaxonomyRequestDto;
import com.dto.skill_dto.SkillTaxonomyResponseDto;
import com.dto.skill_dto.SkillTaxonomyTreeDto;
import com.dto.skill_dto.SubSkillTaxoDto;
import com.entity.skill_entities.SkillCategory;
import com.service_interface.skill_service_interface.SkillCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/skill-categories")
@RequiredArgsConstructor
public class SkillCategoryController {

    private final SkillCategoryService service;

    @PostMapping
    public ResponseEntity<ApiResponse<SkillCategory>> create(
            @RequestBody SkillCategory category) {

        SkillCategory created = service.create(
                category.getName(),
                category.getDescription()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillCategory>>> getAll() {
        List<SkillCategory> categories = service.findAll();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<SkillCategory>>> getActiveCategories() {
        List<SkillCategory> categories = service.findActiveCategories();
        return ResponseEntity.ok(ApiResponse.success("Active categories retrieved successfully", categories));
    }

    @PostMapping("/{categoryId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateCategory(@PathVariable UUID categoryId) {
        service.deactivateCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category deactivated successfully"));
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<SkillTaxonomyTreeDto>>> getSkillTaxonomyTree() {
        List<SkillTaxonomyTreeDto> treeDto = service.getSkillTaxonomyTree();
        return ResponseEntity.ok(ApiResponse.success("Skill taxonomy tree retrieved successfully", treeDto));
    }

    @GetMapping("/tree/{categoryId}")
    public ResponseEntity<ApiResponse<SkillTaxonomyTreeDto>> getSkillTaxonomyTreeByCategory(@PathVariable UUID categoryId) {
        SkillTaxonomyTreeDto treeDto = service.getSkillTaxonomyTreeByCategoryId(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Skill taxonomy tree retrieved successfully", treeDto));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SkillSearchResultDto>>> searchSkills(
            @RequestParam String query) {
        
        // ========================================================================
        // INPUT VALIDATION
        // ========================================================================
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Search query cannot be empty"));
        }

        // ========================================================================
        // SEARCH EXECUTION
        // ========================================================================
        List<SkillSearchResultDto> results = service.searchSkills(query.trim());
        
        // ========================================================================
        // RESPONSE HANDLING
        // ========================================================================
        if (results.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("skill not exist"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Skills found successfully", results));
    }

    @GetMapping("/dto")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getAllCategoriesAsDto() {
        List<CategoryDto> categories = service.getAllCategoriesDto();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    @GetMapping("/{categoryId}/skills-dto")
    public ResponseEntity<ApiResponse<List<SkillDto>>> getSkillsByCategoryIdAsDto(@PathVariable UUID categoryId) {
        List<SkillDto> skills = service.getSkillsByCategoryId(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Skills retrieved successfully for category " + categoryId, skills));
    }

    @GetMapping("/skills/{skillId}/subskills-dto")
    public ResponseEntity<ApiResponse<List<SubSkillTaxoDto>>> getSubSkillsBySkillIdAsDto(@PathVariable UUID skillId) {
        List<SubSkillTaxoDto> subSkills = service.getSubSkillsBySkillId(skillId);
        return ResponseEntity.ok(ApiResponse.success("SubSkills retrieved successfully for skill " + skillId, subSkills));
    }

    @PostMapping("/taxonomy")
    public ResponseEntity<ApiResponse<SkillTaxonomyResponseDto>> manageSkillTaxonomy(
            @RequestBody SkillTaxonomyRequestDto requestDto) {
        SkillTaxonomyResponseDto responseDto = service.manageSkillTaxonomy(requestDto);
        return ResponseEntity.ok(ApiResponse.success("Skill taxonomy processed successfully", responseDto));
    }
}
