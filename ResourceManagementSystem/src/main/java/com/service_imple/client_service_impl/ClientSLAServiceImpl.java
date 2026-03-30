package com.service_imple.client_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.entity.client_entities.ClientSLA;
import com.entity.project_entities.ProjectSLA;
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
        ClientSLA sla = clientSLARepo.save(clientSLA);
        if (sla != null) {
            return ResponseEntity.ok(ApiResponse.success("Client SLA Created Successfully", sla));
        } else {
            throw new ClientExceptionHandler("Client Sla creation Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<ClientSLA>> updateClientSLA(ClientSLA clientSLA) {
        ClientSLA sla = clientSLARepo.save(clientSLA);
        if (sla != null) {
            return ResponseEntity.ok(ApiResponse.success("Client SLA Updated Successfully", sla));
        } else {
            throw new ClientExceptionHandler("Client Sla Update Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<ClientSLA>> deleteClientSLA(UUID id) {
        ClientSLA sla = clientSLARepo.findById(id)
                .orElseThrow(() -> new ClientExceptionHandler("Client SLA not found"));

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
        List<ClientSLA> sla = clientSLARepo.findAllByClient_ClientId(clientId).orElseThrow(() -> new ClientExceptionHandler("Failed to fetch client sla"));
        return ResponseEntity.ok(ApiResponse.success("Client SLA Fetched Successfully", sla));
    }
}
