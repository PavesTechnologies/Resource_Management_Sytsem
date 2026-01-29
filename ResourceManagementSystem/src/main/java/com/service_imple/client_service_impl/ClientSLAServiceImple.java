package com.service_imple.client_service_impl;

import com.controller.client_controllers.ClientSLAController;
import com.dto.ApiResponse;
import com.entity.client_entities.ClientSLA;
import com.global_exception_handler.ClientException;
import com.repo.client_repo.ClientSLARepo;
import com.service_interface.client_service_interface.ClientSLAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class ClientSLAServiceImple implements ClientSLAService {


    @Autowired
    ClientSLARepo clientSLARepo;

    @Autowired
    ApiResponse apiResponse;
    @Override
    public ResponseEntity<ApiResponse> createClientSLA(ClientSLA clientSLA) {
        ClientSLA sla=clientSLARepo.save(clientSLA);
        ApiResponse<ClientSLA> apiResponse= new ApiResponse<>();
        if(sla!=null) {
            return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client SLA Created Successfully",sla));
        }
        else {
            throw new ClientException("Clinet Sla creation Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse> updateClientSLA(ClientSLA clientSLA) {
        ClientSLA sla=clientSLARepo.save(clientSLA);
        if(sla!=null) {
            return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client SLA Updated Successfully",sla));
        }
        else {
            throw new ClientException("Clinet Sla Update Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse> deleteClientSLA(Long id) {
        ClientSLA sla=clientSLARepo.findById(id).get();
        if(sla!=null) {
            return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client SLA Deleted Successfully",sla));
        }
        else {
            throw new ClientException("Clinet Sla Deletion Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getClientSLA(Long clientId) {
        List<ClientSLA> sla=clientSLARepo.findAllByClient_ClientId(clientId).orElseThrow(() -> new ClientException("Failed to fentch client sla"));
        return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client SLA Fentched Successfully",sla));
    }
}
