package com.service_interface.project_service_interface;

import com.dto.project_dto.ProjectEscalationDTO;
import com.dto.project_dto.ProjectEscalationResponseDTO;
import com.entity.project_entities.ProjectEscalation;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ProjectEscalationService {
    ResponseEntity<?> addEscalationContact(ProjectEscalationResponseDTO escalation);
    ResponseEntity<?> getEscalationContacts(Long projectId);
//    void removeEscalationContact(java.util.UUID escalationId);
}
