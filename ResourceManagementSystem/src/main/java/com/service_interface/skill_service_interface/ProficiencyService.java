package com.service_interface.skill_service_interface;

import com.entity.skill_entities.ProficiencyLevel;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface ProficiencyService {

    ResponseEntity<?> createProficiencyLevel(ProficiencyLevel proficiencyLevel);
    ResponseEntity<?> updateProficiencyLevel(ProficiencyLevel proficiencyLevel, UUID proficiencyId);
    ResponseEntity<?> getAllProficiencyLevels();
    ResponseEntity<?> deleteProficiencyLevel(UUID proficiencyId);
}
