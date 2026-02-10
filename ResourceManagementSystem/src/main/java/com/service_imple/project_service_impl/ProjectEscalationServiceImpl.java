package com.service_imple.project_service_impl;

import com.dto.project_dto.ProjectEscalationDTO;
import com.entity.client_entities.Client;
import com.entity.client_entities.ClientEscalationContact;
import com.entity.project_entities.Project;
import com.entity.project_entities.ProjectEscalation;
import com.repo.client_repo.ClientContactRepo;
import com.repo.client_repo.ClientRepo;
import com.repo.project_repo.ProjectEscalationRepo;
import com.repo.project_repo.ProjectRepository;
import com.service_interface.project_service_interface.ProjectEscalationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
    private final ClientRepo clientRepo;

    @Override
    @Transactional
    public ProjectEscalationDTO addEscalationContact(ProjectEscalationDTO dto) {
        if (dto.getTriggers() == null || dto.getTriggers().isEmpty()) {
            throw new IllegalArgumentException("At least one trigger must be selected.");
        }

        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        ClientEscalationContact contact;

        if (dto.getContactId() != null) {
            // Use existing contact
            contact = clientContactRepo.findById(dto.getContactId())
                    .orElseThrow(() -> new EntityNotFoundException("Contact not found"));
        } else {
            // Create new contact
            if (dto.getClientId() == null) {
                // If clientId is not provided in DTO, try to get it from the project
                if (project.getClientId() != null) {
                    dto.setClientId(project.getClientId());
                } else {
                    throw new IllegalArgumentException("Client ID is required to create a new contact.");
                }
            }

            Client client = clientRepo.findById(dto.getClientId())
                    .orElseThrow(() -> new EntityNotFoundException("Client not found"));

            contact = ClientEscalationContact.builder()
                    .client(client)
                    .contactName(dto.getContactName())
                    .email(dto.getContactEmail())
                    .phone(dto.getContactPhone())
                    .contactRole(dto.getContactRole())
                    .activeFlag(dto.getActiveFlag() != null ? dto.getActiveFlag() : true)
                    .build();

            contact = clientContactRepo.save(contact);
        }

        // Check for duplicates: same contact at same level for same project
        UUID contactIdToCheck = contact.getContactId();
        boolean exists = projectEscalationRepo.findByProject_PmsProjectId(dto.getProjectId()).stream()
                .anyMatch(pe -> pe.getContact().getContactId().equals(contactIdToCheck) 
                        && pe.getEscalationLevel() == dto.getEscalationLevel());

        if (exists) {
            throw new IllegalArgumentException("Contact is already mapped with this escalation level for this project.");
        }

        ProjectEscalation projectEscalation = ProjectEscalation.builder()
                .project(project)
                .contact(contact)
                .escalationLevel(dto.getEscalationLevel())
                .triggers(dto.getTriggers())
                .build();

        ProjectEscalation saved = projectEscalationRepo.save(projectEscalation);

        return mapToDTO(saved);
    }

    @Override
    public List<ProjectEscalationDTO> getEscalationContacts(Long projectId) {
        return projectEscalationRepo.findByProject_PmsProjectId(projectId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeEscalationContact(UUID escalationId) {
        projectEscalationRepo.deleteById(escalationId);
    }

    private ProjectEscalationDTO mapToDTO(ProjectEscalation entity) {
        return ProjectEscalationDTO.builder()
                .id(entity.getId())
                .projectId(entity.getProject().getPmsProjectId())
                .clientId(entity.getContact().getClient().getClientId())
                .contactId(entity.getContact().getContactId())
                .contactName(entity.getContact().getContactName())
                .contactEmail(entity.getContact().getEmail())
                .contactPhone(entity.getContact().getPhone())
                .activeFlag(entity.getContact().getActiveFlag())
                .contactRole(entity.getContact().getContactRole())
                .escalationLevel(entity.getEscalationLevel())
                .triggers(entity.getTriggers())
                .build();
    }
}
