package com.service_interface.skill_service_interface;

import com.dto.centralised_dto.ApiResponse;
import com.entity.skill_entities.ProficiencyLevel;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface ProficiencyService {

    ResponseEntity<ApiResponse<ProficiencyLevel>> createProficiencyLevel(ProficiencyLevel proficiencyLevel);
    ResponseEntity<ApiResponse<ProficiencyLevel>> updateProficiencyLevel(ProficiencyLevel proficiencyLevel, UUID proficiencyId);
    ResponseEntity<ApiResponse<?>> getAllProficiencyLevels();
    ResponseEntity<ApiResponse<Void>> deleteProficiencyLevel(UUID proficiencyId);
}
