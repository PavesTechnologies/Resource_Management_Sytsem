package com.controller.skill_controllers;

import com.service_interface.skill_service_interface.ProficiencyLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proficiency")
@CrossOrigin
public class ProficiencyController {

    @Autowired
    ProficiencyLevel proficiencyLevel;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProficiencyLevel(@RequestBody ProficiencyLevel proficiencyLevel) {
        return proficiencyLevel.createProficiencyLevel(proficiencyLevel);
    }
}
