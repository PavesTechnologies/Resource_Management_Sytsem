package com.service_imple.project_service_impl;

import com.dto.ApiResponse;
import com.dto.project_dto.ProjectEscalationResponseDTO;
import com.entity.client_entities.ClientEscalationContact;
import com.entity.project_entities.Project;
import com.entity.project_entities.ProjectEscalation;
import com.entity_enums.project_enums.EscalationSource;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.client_repo.ClientContactRepo;
import com.repo.project_repo.ProjectEscalationRepo;
import com.repo.project_repo.ProjectRepository;
import com.service_interface.project_service_interface.ProjectEscalationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectEscalationServiceImpl implements ProjectEscalationService {

    private final ProjectEscalationRepo projectEscalationRepo;
    private final ClientContactRepo clientContactRepo;
    private final ProjectRepository projectRepository;

    @Override
    @Transactional
    public ResponseEntity<?> addEscalationContact(
            ProjectEscalationResponseDTO projectEscalation) {

        Project project = projectRepository.findById(projectEscalation.getProjectId()).orElseThrow(() -> new ProjectExceptionHandler(HttpStatus.NOT_FOUND, "404", "Project Not Found!"));

        if ("INHERIT".equalsIgnoreCase(projectEscalation.getType())) {
            inheritFromClient(projectEscalation, project);
        }
        else if ("MANUAL".equalsIgnoreCase(projectEscalation.getType())) {
            createManualEscalation(projectEscalation, project);
        }
        else {
            throw new ProjectExceptionHandler(HttpStatus.BAD_REQUEST, "400", "Invalid escalation type");
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Project Escalation created successfully!", null));
    }

    private void inheritFromClient(ProjectEscalationResponseDTO dto, Project project) {

        if (dto.getContactId() == null || dto.getContactId().isEmpty()) {
            throw new ProjectExceptionHandler(HttpStatus.BAD_REQUEST, "400", "Contact IDs are required for inherit type");
        }

        List<ClientEscalationContact> clientContacts =
                clientContactRepo.findAllById(dto.getContactId());

        if (clientContacts.isEmpty()) {
            throw new ProjectExceptionHandler(HttpStatus.BAD_REQUEST, "400", "No matching client escalation contacts found");
        }

        List<ProjectEscalation> projectEscalations = clientContacts.stream()
                .map(clientContact -> ProjectEscalation.builder()
                        .project(project)
                        .escalationLevel(dto.getEscalationLevel())
                        .contactName(clientContact.getContactName())
                        .contactRole(clientContact.getContactRole())
                        .email(clientContact.getEmail())
                        .phone(clientContact.getPhone())
                        .activeFlag(Boolean.TRUE)
                        .source(EscalationSource.INHERITED)
                        .build())
                .toList();

        projectEscalationRepo.saveAll(projectEscalations);
    }

    /* ---------------------------------------------------------
       MANUAL FLOW
       --------------------------------------------------------- */
    private void createManualEscalation(ProjectEscalationResponseDTO dto, Project project) {

        if (dto.getContactName() == null || dto.getEmail() == null) {
            throw new ProjectExceptionHandler(HttpStatus.BAD_REQUEST, "400", "Contact details are required for manual escalation");
        }

        ProjectEscalation escalation = ProjectEscalation.builder()
                .project(project)
                .escalationLevel(dto.getEscalationLevel())
                .contactName(dto.getContactName())
                .contactRole(dto.getContactRole())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .activeFlag(dto.getActiveFlag() != null ? dto.getActiveFlag() : Boolean.TRUE)
                .source(EscalationSource.MANUAL)
                .build();

        projectEscalationRepo.save(escalation);
    }

    @Override
    public ResponseEntity<?> updateProjectContact(UUID projectEscalationId, ProjectEscalation escalation) {
        ProjectEscalation projectEscalation = projectEscalationRepo.findById(projectEscalationId).orElseThrow(() -> new ProjectExceptionHandler(HttpStatus.NOT_FOUND, "400", "Project Escalation Not Found!"));
        projectEscalationRepo.save(escalation);
        return ResponseEntity.ok(new ApiResponse<>(true, "Project Escalation updated successfully!", null));
    }

    @Override
    public ResponseEntity<?> deleteProjectContact(UUID projectEscalationId) {
        projectEscalationRepo.findById(projectEscalationId).orElseThrow(() -> new ProjectExceptionHandler(HttpStatus.NOT_FOUND, "400", "Project Escalation Not Found!"));
        projectEscalationRepo.deleteById(projectEscalationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Project Escalation deleted successfully!", null));
    }

    @Override
    public ResponseEntity<?> getEscalationContacts(Long projectId) {
        List<ProjectEscalation> projectEscalations = projectEscalationRepo.findByProject_PmsProjectId(projectId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Escalation contacts retrieved successfully", projectEscalations));
    }
//
//    @Override
//    @Transactional
//    public void removeEscalationContact(UUID escalationId) {
//        projectEscalationRepo.deleteById(escalationId);
//    }
//
//    private ProjectEscalationDTO mapToDTO(ProjectEscalation entity) {
//        return ProjectEscalationDTO.builder()
//                .id(entity.getId())
//                .projectId(entity.getProject().getPmsProjectId())
//                .contactId(entity.getContact().getContactId())
//                .contactName(entity.getContact().getContactName())
//                .contactEmail(entity.getContact().getEmail())
//                .contactRole(entity.getContact().getContactRole())
//                .escalationLevel(entity.getEscalationLevel())
//                .triggers(entity.getTriggers())
//                .build();
//    }
}
