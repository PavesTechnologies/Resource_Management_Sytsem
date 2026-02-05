package com.service_imple.client_service_impl;

import com.dto.ApiResponse;
import com.entity.client_entities.ClientSLA;
import com.global_exception_handler.ClientExceptionHandler;
import com.repo.client_repo.ClientSLARepo;
import com.service_interface.client_service_interface.ClientSLAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ClientSLAServiceImple implements ClientSLAService {


    @Autowired
    ClientSLARepo clientSLARepo;

    @Autowired
    ApiResponse apiResponse;
    
    @Override
    public ResponseEntity<ApiResponse<ClientSLA>> createClientSLA(ClientSLA clientSLA) {
        ClientSLA sla=clientSLARepo.save(clientSLA);
        ApiResponse<ClientSLA> response = new ApiResponse<>();
        if(sla!=null) {
            return ResponseEntity.ok(response.getAPIResponse(true,"Client SLA Created Successfully",sla));
        }
        else {
            throw new ClientExceptionHandler("Clinet Sla creation Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<ClientSLA>> updateClientSLA(ClientSLA clientSLA) {
        ClientSLA sla=clientSLARepo.save(clientSLA);
        ApiResponse<ClientSLA> response = new ApiResponse<>();
        if(sla!=null) {
            return ResponseEntity.ok(response.getAPIResponse(true,"Client SLA Updated Successfully",sla));
        }
        else {
            throw new ClientExceptionHandler("Clinet Sla Update Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<ClientSLA>> deleteClientSLA(UUID id) {
        ClientSLA sla=clientSLARepo.findById(id).orElseThrow(() -> new ClientExceptionHandler("Client SLA not found"));
        clientSLARepo.deleteById(id);
        ApiResponse<ClientSLA> response = new ApiResponse<>();
        return ResponseEntity.ok(response.getAPIResponse(true,"Client SLA Deleted Successfully",sla));
    }

    @Override
    public ResponseEntity<ApiResponse<List<ClientSLA>>> getClientSLA(UUID clientId) {
        List<ClientSLA> sla=clientSLARepo.findAllByClient_ClientId(clientId).orElseThrow(() -> new ClientExceptionHandler("Failed to fentch client sla"));
        ApiResponse<List<ClientSLA>> response = new ApiResponse<>();
        return ResponseEntity.ok(response.getAPIResponse(true,"Client SLA Fentched Successfully",sla));
    }
}
