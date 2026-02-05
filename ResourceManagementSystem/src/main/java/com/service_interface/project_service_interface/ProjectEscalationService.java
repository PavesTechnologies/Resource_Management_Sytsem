package com.service_interface.project_service_interface;

import com.dto.project_dto.ProjectEscalationDTO;

import java.util.List;

public interface ProjectEscalationService {
    ProjectEscalationDTO addEscalationContact(ProjectEscalationDTO escalationDTO);
    List<ProjectEscalationDTO> getEscalationContacts(Long projectId);
    void removeEscalationContact(java.util.UUID escalationId);
}
