package com.controller.skill_controllers;

import com.entity.skill_entities.ProficiencyLevel;
import com.service_interface.skill_service_interface.ProficiencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/proficiency")
@CrossOrigin
public class ProficiencyController {

    @Autowired
    ProficiencyService proficiencyService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProficiencyLevel(@RequestBody ProficiencyLevel proficiencyLevel) {
        return proficiencyService.createProficiencyLevel(proficiencyLevel);
    }

    @PutMapping("/update/{proficiencyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProficiencyLevel(@RequestBody ProficiencyLevel proficiencyLevel, @PathVariable UUID proficiencyId) {
        return proficiencyService.updateProficiencyLevel(proficiencyLevel, proficiencyId);
    }

    @GetMapping("/get-all-proficiency-levels")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER')")
    public ResponseEntity<?> getAllProficiencyLevels() {
        return proficiencyService.getAllProficiencyLevels();
    }

    @DeleteMapping("/delete/{proficiencyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProficiencyLevel(@PathVariable UUID proficiencyId) {
        return proficiencyService.deleteProficiencyLevel(proficiencyId);
    }
}
