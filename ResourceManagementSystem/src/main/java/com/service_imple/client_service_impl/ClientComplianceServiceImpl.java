package com.service_imple.client_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.entity.client_entities.ClientCompliance;
import com.entity_enums.client_enums.RequirementType;
import com.global_exception_handler.ClientExceptionHandler;
import com.global_exception_handler.ProjectExceptionHandler;
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
            throw new ProjectExceptionHandler( HttpStatus.BAD_REQUEST,
                    "BAD_REQUEST",
                    "Skill must be selected for SKILL requirement type"
            );
        }

        if (clientCompliance.getRequirementType() == RequirementType.CERTIFICATION
                && clientCompliance.getCertificate() == null) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "Certificate must be selected for CERTIFICATION requirement type",
                    null
            );
        }

        ClientCompliance Compliance=clientComplianceRepo.save(clientCompliance);
        ApiResponse<ClientCompliance> apiResponse= new ApiResponse<>();
        if(Compliance!=null) {
            return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client Compliance Created Successfully",Compliance));
        }
        else {
            throw new ClientExceptionHandler("Client Compliance creation Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> updateClientCompliance(ClientCompliance clientCompliance) {
        ClientCompliance Compliance=clientComplianceRepo.save(clientCompliance);
        if(Compliance!=null) {
            return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client Compliance Updated Successfully",Compliance));
        }
        else {
            throw new ClientExceptionHandler("Clinet Compliance Update Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> deleteClientCompliance(UUID id) {
        ClientCompliance Compliance=clientComplianceRepo.findById(id).get();
        if(Compliance!=null) {
            return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client Compliance Deleted Successfully",Compliance));
        }
        else {
            throw new ClientExceptionHandler("Clinet Compliance Deletion Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getClientCompliance(UUID clientId) {
        List<ClientCompliance> Compliance=clientComplianceRepo.findAllByClient_ClientId(clientId).orElseThrow(() -> new ClientExceptionHandler("Failed to fentch client Compliance"));

        return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client Compliance Fentched Successfully",Compliance));

    }
}
