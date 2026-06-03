package com.service_imple.skill_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.entity.skill_entities.ProficiencyLevel;
import com.global_exception_handler.SkillExceptionHandler;
import com.repo.skill_repo.ProficiencyLevelRepository;
import com.service_interface.skill_service_interface.ProficiencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProficiencyServiceImp implements ProficiencyService {

    private final ProficiencyLevelRepository proficiencyLevelRepository;

    @Override
    public ResponseEntity<ApiResponse<ProficiencyLevel>> createProficiencyLevel(ProficiencyLevel proficiencyLevel) {
        log.info("Creating proficiency level: {}", proficiencyLevel.getProficiencyName());
        ProficiencyLevel proficiency = proficiencyLevelRepository.save(proficiencyLevel);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Created successfully", proficiency));
    }

    @Override
    public ResponseEntity<ApiResponse<ProficiencyLevel>> updateProficiencyLevel(ProficiencyLevel proficiencyLevel, UUID id) {
        log.info("Updating proficiency level with ID: {}", id);
        proficiencyLevelRepository.findById(id).orElseThrow(() -> SkillExceptionHandler.notFound("Proficiency with the ID is Not found!"));
        ProficiencyLevel proficiency = proficiencyLevelRepository.save(proficiencyLevel);
        return ResponseEntity.ok(ApiResponse.success("Updated successfully", proficiency));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllProficiencyLevels() {
        return ResponseEntity.ok(ApiResponse.success("Data fetched successfully", proficiencyLevelRepository.findAll()));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteProficiencyLevel(UUID id) {
        log.info("Deleting proficiency level with ID: {}", id);
        proficiencyLevelRepository.findById(id).orElseThrow(() -> SkillExceptionHandler.notFound("Proficiency with the ID is Not found!"));
        proficiencyLevelRepository.deleteById(id);
        log.info("Proficiency level deleted successfully with ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Deleted successfully"));
    }

}
