package com.controller.skill_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.entity.skill_entities.ProficiencyLevel;
import com.service_interface.skill_service_interface.ProficiencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/proficiency")
@RequiredArgsConstructor
public class ProficiencyController {

    private final ProficiencyService proficiencyService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<ProficiencyLevel>> createProficiencyLevel(@RequestBody ProficiencyLevel proficiencyLevel) {
        return proficiencyService.createProficiencyLevel(proficiencyLevel);
    }

    @PutMapping("/update/{proficiencyId}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<ProficiencyLevel>> updateProficiencyLevel(@RequestBody ProficiencyLevel proficiencyLevel, @PathVariable UUID proficiencyId) {
        return proficiencyService.updateProficiencyLevel(proficiencyLevel, proficiencyId);
    }

    @GetMapping("/get-all-proficiency-levels")
    @PreAuthorize("hasAnyRole('General')")
    public ResponseEntity<ApiResponse<?>> getAllProficiencyLevels() {
        return proficiencyService.getAllProficiencyLevels();
    }

    @DeleteMapping("/delete/{proficiencyId}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<Void>> deleteProficiencyLevel(@PathVariable UUID proficiencyId) {
        return proficiencyService.deleteProficiencyLevel(proficiencyId);
    }
}
