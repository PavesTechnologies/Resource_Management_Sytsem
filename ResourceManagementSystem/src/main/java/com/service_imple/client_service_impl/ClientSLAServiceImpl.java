package com.service_imple.client_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.entity.client_entities.ClientSLA;
import com.entity.project_entities.ProjectSLA;
import com.entity_enums.client_enums.SLAType;
import com.global_exception_handler.ClientExceptionHandler;
import com.repo.client_repo.ClientSLARepo;
import com.repo.project_repo.ProjectSLARepo;
import com.service_interface.client_service_interface.ClientSLAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ClientSLAServiceImpl implements ClientSLAService {


    @Autowired
    ClientSLARepo clientSLARepo;

    @Autowired
    ProjectSLARepo projectSLARepo;

    @Override
    public ResponseEntity<ApiResponse<ClientSLA>> createClientSLA(ClientSLA clientSLA) {
        // Validate duration days > warning threshold days
        if (clientSLA.getSlaDurationDays() != null && clientSLA.getWarningThresholdDays() != null) {
            if (clientSLA.getWarningThresholdDays() >= clientSLA.getSlaDurationDays()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Warning threshold days must be less than duration days", null));
            }
        }
        
        // Check for duplicate SLA types (NET_NEW, REPLACEMENT, EMERGENCY)
        if (clientSLA.getClient() != null && clientSLA.getSlaType() != null) {
            SLAType slaType = clientSLA.getSlaType();
            if (slaType == SLAType.NET_NEW || slaType == SLAType.REPLACEMENT || slaType == SLAType.EMERGENCY || slaType == SLAType.BACKFILL) {
                java.util.Optional<ClientSLA> existingSLA = clientSLARepo.findByClient_ClientIdAndSlaType(
                        clientSLA.getClient().getClientId(), slaType);
                
                if (existingSLA.isPresent()) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("SLA type " + slaType + " already exists for this client. Only one instance allowed.", null));
                }
            }
        }
        
        ClientSLA sla = clientSLARepo.save(clientSLA);
        if (sla != null) {
            return ResponseEntity.ok(ApiResponse.success("Client SLA Created Successfully", sla));
        } else {
            throw ClientExceptionHandler.badRequest("Client Sla creation Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<ClientSLA>> updateClientSLA(ClientSLA clientSLA) {
        // Validate duration days > warning threshold days
        if (clientSLA.getSlaDurationDays() != null && clientSLA.getWarningThresholdDays() != null) {
            if (clientSLA.getWarningThresholdDays() >= clientSLA.getSlaDurationDays()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Warning threshold days must be less than duration days", null));
            }
        }
        
        // Check for duplicate SLA types (NET_NEW, REPLACEMENT, EMERGENCY) during update
        if (clientSLA.getClient() != null && clientSLA.getSlaType() != null) {
            SLAType slaType = clientSLA.getSlaType();
            if (slaType == SLAType.NET_NEW || slaType == SLAType.REPLACEMENT || slaType == SLAType.EMERGENCY || slaType == SLAType.BACKFILL) {
                java.util.Optional<ClientSLA> existingSLA = clientSLARepo.findByClient_ClientIdAndSlaType(
                        clientSLA.getClient().getClientId(), slaType);
                
                // If there's an existing SLA and it's not the same one being updated
                if (existingSLA.isPresent() && !existingSLA.get().getSlaId().equals(clientSLA.getSlaId())) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("SLA type " + slaType + " already exists for this client. Only one instance allowed.", null));
                }
            }
        }
        
        ClientSLA sla = clientSLARepo.save(clientSLA);
        if (sla != null) {
            return ResponseEntity.ok(ApiResponse.success("Client SLA Updated Successfully", sla));
        } else {
            throw ClientExceptionHandler.badRequest("Client Sla Update Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<ClientSLA>> deleteClientSLA(UUID id) {
        ClientSLA sla = clientSLARepo.findById(id)
                .orElseThrow(() -> ClientExceptionHandler.badRequest("Client SLA not found"));

        boolean isActiveInProjects = projectSLARepo.existsByClientSLA_SlaIdAndActiveFlagTrue(id);

        if (isActiveInProjects) {
            List<ProjectSLA> activeProjectSLAs = projectSLARepo.findByClientSLA_SlaIdAndActiveFlagTrue(id);
            StringBuilder projectNames = new StringBuilder();

            for (ProjectSLA projectSLA : activeProjectSLAs) {
                if (projectSLA.getProject() != null) {
                    if (projectNames.length() > 0) {
                        projectNames.append(", ");
                    }
                    projectNames.append(projectSLA.getProject().getName());
                }
            }

            String message = "This particular SLA type is active in the following project(s): " +
                    projectNames + ". Can't delete this SLA type.";

            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(message, null));
        }

        try {
            clientSLARepo.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Client SLA Deleted Successfully", sla));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("This SLA type is referenced by other records and cannot be deleted.", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<List<ClientSLA>>> getClientSLA(UUID clientId) {
        List<ClientSLA> sla = clientSLARepo.findAllByClient_ClientId(clientId).orElseThrow(() -> ClientExceptionHandler.badRequest("Failed to fetch client sla"));
        return ResponseEntity.ok(ApiResponse.success("Client SLA Fetched Successfully", sla));
    }
}
