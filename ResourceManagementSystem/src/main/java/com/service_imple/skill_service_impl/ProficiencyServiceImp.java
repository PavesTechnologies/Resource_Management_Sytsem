package com.service_imple.skill_service_impl;

import com.dto.ApiResponse;
import com.entity.skill_entities.ProficiencyLevel;
import com.global_exception_handler.ProficiencyExceptionHandler;
import com.repo.skill_repo.ProficiencyRepo;
import com.service_interface.skill_service_interface.ProficiencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class ProficiencyServiceImp implements ProficiencyService {

    @Autowired
    ProficiencyRepo proficiencyRepo;

    @Override
    @CacheEvict(value = "proficiencyLevels", allEntries = true)
    public ResponseEntity<?> createProficiencyLevel(ProficiencyLevel proficiencyLevel) {
        log.info("Creating proficiency level: {}", proficiencyLevel.getProficiencyName());
        ProficiencyLevel proficiency = proficiencyRepo.save(proficiencyLevel);
        //log.info("Proficiency level created successfully with ID: {}", proficiency.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Proficiency level created successfully", proficiency));
    }

    @Override
    @CacheEvict(value = "proficiencyLevels", allEntries = true)
    public ResponseEntity<?> updateProficiencyLevel(ProficiencyLevel proficiencyLevel, UUID id) {
        log.info("Updating proficiency level with ID: {}", id);
        proficiencyRepo.findById(id).orElseThrow(() -> ProficiencyExceptionHandler.notFound("Proficiency with the ID is Not found!"));
        ProficiencyLevel proficiency = proficiencyRepo.save(proficiencyLevel);
        //log.info("Proficiency level updated successfully with ID: {}", proficiency.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Proficiency level updated successfully", proficiency));
    }

    @Override
    public ResponseEntity<?> getAllProficiencyLevels() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Proficiency levels retrieved successfully", proficiencyRepo.findAll()));
    }

    @Override
    @CacheEvict(value = "proficiencyLevels", allEntries = true)
    public ResponseEntity<?> deleteProficiencyLevel(UUID id) {
        log.info("Deleting proficiency level with ID: {}", id);
        proficiencyRepo.findById(id).orElseThrow(() -> ProficiencyExceptionHandler.notFound("Proficiency with the ID is Not found!"));
        proficiencyRepo.deleteById(id);
        log.info("Proficiency level deleted successfully with ID: {}", id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Proficiency level deleted successfully", null));
    }

}
