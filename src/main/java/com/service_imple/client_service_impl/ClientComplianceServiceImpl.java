package com.service_imple.client_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.entity.client_entities.ClientCompliance;
import com.entity_enums.client_enums.RequirementType;
import com.global_exception_handler.ClientExceptionHandler;
import com.repo.client_repo.ClientComplianceRepo;
import com.service_interface.client_service_interface.ClientComplianceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ClientComplianceServiceImpl implements ClientComplianceService {
    @Autowired
    ClientComplianceRepo clientComplianceRepo;

    @Autowired
    ApiResponse apiResponse;
    @Override
    public ResponseEntity<ApiResponse<?>> createClientCompliance(ClientCompliance clientCompliance) {
        if (clientCompliance.getRequirementType() == RequirementType.SKILL
                && clientCompliance.getSkill() == null) {
            throw new ClientExceptionHandler( HttpStatus.BAD_REQUEST,
                    "BAD_REQUEST",
                    "Skill must be selected for SKILL requirement type"
            );
        }

        if (clientCompliance.getRequirementType() == RequirementType.CERTIFICATION
                && clientCompliance.getCertificate() == null) {
            throw new ClientExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "Certificate must be selected for CERTIFICATION requirement type",
                    null
            );
        }

        // Check for duplicate requirement name within the same client
        if (clientCompliance.getClient() != null && clientCompliance.getRequirementName() != null) {
            if (clientComplianceRepo.existsByClient_ClientIdAndRequirementName(
                    clientCompliance.getClient().getClientId(), clientCompliance.getRequirementName())) {
                throw new ClientExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "Requirement name already exists for this client",
                        null
                );
            }
        }

        // Check for duplicate certificate within the same client
        if (clientCompliance.getRequirementType() == RequirementType.CERTIFICATION 
                && clientCompliance.getClient() != null && clientCompliance.getCertificate() != null) {
            if (clientComplianceRepo.existsByClient_ClientIdAndCertificate(
                    clientCompliance.getClient().getClientId(), clientCompliance.getCertificate())) {
                throw new ClientExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "Certificate already exists for this client",
                        null
                );
            }
        }

        // Check for duplicate skill within the same client
        if (clientCompliance.getRequirementType() == RequirementType.SKILL 
                && clientCompliance.getClient() != null && clientCompliance.getSkill() != null) {
            if (clientComplianceRepo.existsByClient_ClientIdAndSkill(
                    clientCompliance.getClient().getClientId(), clientCompliance.getSkill())) {
                throw new ClientExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "Skill already exists for this client",
                        null
                );
            }
        }

        ClientCompliance Compliance=clientComplianceRepo.save(clientCompliance);
        ApiResponse<ClientCompliance> apiResponse= new ApiResponse<>();
        if(Compliance!=null) {
            return ResponseEntity.ok(ApiResponse.success("Client Pre-requisite Created Successfully",Compliance));
        }
        else {
            throw ClientExceptionHandler.badRequest("Client Pre-requisite creation Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> updateClientCompliance(ClientCompliance clientCompliance) {
        // Check for duplicate requirement name (excluding current compliance)
        if (clientCompliance.getClient() != null && clientCompliance.getRequirementName() != null) {
            if (clientComplianceRepo.findByRequirementNameExcludingId(
                    clientCompliance.getClient().getClientId(), 
                    clientCompliance.getRequirementName(),
                    clientCompliance.getComplianceId()).isPresent()) {
                throw new ClientExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "Requirement name already exists for this client",
                        null
                );
            }
        }

        // Check for duplicate certificate (excluding current compliance)
        if (clientCompliance.getRequirementType() == RequirementType.CERTIFICATION 
                && clientCompliance.getClient() != null && clientCompliance.getCertificate() != null) {
            if (clientComplianceRepo.findByCertificateExcludingId(
                    clientCompliance.getClient().getClientId(), 
                    clientCompliance.getCertificate(),
                    clientCompliance.getComplianceId()).isPresent()) {
                throw new ClientExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "Certificate already exists for this client",
                        null
                );
            }
        }

        // Check for duplicate skill (excluding current compliance)
        if (clientCompliance.getRequirementType() == RequirementType.SKILL 
                && clientCompliance.getClient() != null && clientCompliance.getSkill() != null) {
            if (clientComplianceRepo.findBySkillExcludingId(
                    clientCompliance.getClient().getClientId(), 
                    clientCompliance.getSkill(),
                    clientCompliance.getComplianceId()).isPresent()) {
                throw new ClientExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "Skill already exists for this client",
                        null
                );
            }
        }

        ClientCompliance Compliance=clientComplianceRepo.save(clientCompliance);
        if(Compliance!=null) {
            return ResponseEntity.ok(ApiResponse.success("Client Pre-requisite Updated Successfully",Compliance));
        }
        else {
            throw ClientExceptionHandler.badRequest("Client Pre-requisite Update Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> deleteClientCompliance(UUID id) {
        ClientCompliance compliance = clientComplianceRepo.findById(id)
                .orElseThrow(() -> ClientExceptionHandler.badRequest("Client Pre-requisite not found"));
        
        try {
            clientComplianceRepo.delete(compliance);
            return ResponseEntity.ok(ApiResponse.success("Client Pre-requisite Deleted Successfully",compliance));
        } catch (Exception e) {
            throw ClientExceptionHandler.badRequest("Client Pre-requisite Deletion Failed: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getClientCompliance(UUID clientId) {
        List<ClientCompliance> Compliance=clientComplianceRepo.findAllByClient_ClientId(clientId).orElseThrow(() -> ClientExceptionHandler.badRequest("Failed to fentch client Compliance"));

        return ResponseEntity.ok(ApiResponse.success("Client Pre-requisite fetched Successfully",Compliance));

    }
}
