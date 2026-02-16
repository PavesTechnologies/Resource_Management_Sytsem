package com.service_imple.skill_service_impl;

import com.dto.ApiResponse;
import com.entity.skill_entities.ProficiencyLevel;
import com.global_exception_handler.ProficiencyExceptionHandler;
import com.repo.skill_repo.ProficiencyRepo;
import com.service_interface.skill_service_interface.ProficiencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProficiencyServiceImp implements ProficiencyService {

    @Autowired
    ProficiencyRepo proficiencyRepo;

    @Override
    public ResponseEntity<?> createProficiencyLevel(ProficiencyLevel proficiencyLevel) {
        ProficiencyLevel proficiency = proficiencyRepo.save(proficiencyLevel);
        return ResponseEntity.ok(new ApiResponse<>(true, "Proficiency level created successfully", proficiency));
    }

    @Override
    public ResponseEntity<?> updateProficiencyLevel(ProficiencyLevel proficiencyLevel, UUID id) {
        proficiencyRepo.findById(id).orElseThrow(() -> ProficiencyExceptionHandler.notFound("Proficiency with the ID is Not found!"));
        ProficiencyLevel proficiency = proficiencyRepo.save(proficiencyLevel);
        return ResponseEntity.ok(new ApiResponse<>(true, "Proficiency level updated successfully", proficiency));
    }

    @Override
    public ResponseEntity<?> getAllProficiencyLevels() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Proficiency levels retrieved successfully", proficiencyRepo.findAll()));
    }

    @Override
    public ResponseEntity<?> deleteProficiencyLevel(UUID id) {
        proficiencyRepo.findById(id).orElseThrow(() -> ProficiencyExceptionHandler.notFound("Proficiency with the ID is Not found!"));
        proficiencyRepo.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Proficiency level deleted successfully", null));
    }

}
