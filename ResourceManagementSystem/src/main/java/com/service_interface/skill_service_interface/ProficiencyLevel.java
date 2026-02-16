package com.service_interface.skill_service_interface;

import org.springframework.http.ResponseEntity;

public interface ProficiencyLevel {

    ResponseEntity<?> createProficiencyLevel(ProficiencyLevel proficiencyLevel);
}
