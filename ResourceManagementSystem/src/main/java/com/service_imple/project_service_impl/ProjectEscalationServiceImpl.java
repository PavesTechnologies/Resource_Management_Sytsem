package com.service_imple.project_service_impl;

import com.dto.project_dto.ProjectEscalationDTO;
import com.entity.client_entities.ClientEscalationContact;
import com.entity.project_entities.Project;
import com.entity.project_entities.ProjectEscalation;
import com.repo.client_repo.ClientContactRepo;
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

    @Override
    @Transactional
    public ProjectEscalationDTO addEscalationContact(ProjectEscalationDTO dto) {
        if (dto.getTriggers() == null || dto.getTriggers().isEmpty()) {
            throw new IllegalArgumentException("At least one trigger must be selected.");
        }

        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        ClientEscalationContact contact = clientContactRepo.findById(dto.getContactId())
                .orElseThrow(() -> new EntityNotFoundException("Contact not found"));

        // Check for duplicates: same contact at same level for same project
        // Ideally we should check if the contact is already mapped to the project at all, 
        // but the requirement says "prevent duplicate mappings of the same contact at the same escalation level".
        // It also says "A contact cannot be mapped twice with the same escalation level."
        // This implies a contact COULD be mapped twice with DIFFERENT escalation levels? 
        // "A contact may have different triggers than another contact at the same escalation level."
        // Let's assume a contact + level combination must be unique for a project.
        
        boolean exists = projectEscalationRepo.findByProject_PmsProjectId(dto.getProjectId()).stream()
                .anyMatch(pe -> pe.getContact().getContactId().equals(dto.getContactId()) 
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
                .contactId(entity.getContact().getContactId())
                .contactName(entity.getContact().getContactName())
                .contactEmail(entity.getContact().getEmail())
                .contactRole(entity.getContact().getContactRole())
                .escalationLevel(entity.getEscalationLevel())
                .triggers(entity.getTriggers())
                .build();
    }
}
