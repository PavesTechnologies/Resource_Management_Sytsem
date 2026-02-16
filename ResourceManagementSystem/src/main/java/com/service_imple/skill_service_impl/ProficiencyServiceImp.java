package com.service_imple.skill_service_impl;

import com.dto.ApiResponse;
import com.entity.skill_entities.ProficiencyLevel;
import com.repo.skill_repo.ProficiencyRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ProficiencyServiceImp {

    @Autowired
    ProficiencyRepo proficiencyRepo;

    public ResponseEntity<?> createProficiencyLevel(ProficiencyLevel proficiencyLevel) {
        ProficiencyLevel proficiency = proficiencyRepo.save(proficiencyLevel);
        return ResponseEntity.ok(new ApiResponse<>(true, "Proficiency level created successfully", proficiency));
    }

}
