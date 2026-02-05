package com.service_imple.client_service_impl;


import com.dto.ApiResponse;
import com.entity.client_entities.ClientEscalationContact;
import com.global_exception_handler.ClientExceptionHandler;
import com.repo.client_repo.ClientContactRepo;
import com.service_interface.client_service_interface.ClientContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ClientContactServiceImple implements ClientContactService {
    @Autowired
    ClientContactRepo clientContactRepo;

    @Autowired
    ApiResponse apiResponse;
    @Override
    public ResponseEntity<ApiResponse> createClientContact(ClientEscalationContact clientContact) {
        ClientEscalationContact Contact=clientContactRepo.save(clientContact);
        if(Contact!=null) {
            return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client Contact Created Successfully",Contact));
        }
        else {
            throw new ClientExceptionHandler("Client Contact creation Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse> updateClientContact(ClientEscalationContact clientContact) {
        ClientEscalationContact Contact=clientContactRepo.save(clientContact);
        if(Contact!=null) {
            return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client Contact Updated Successfully",Contact));
        }
        else {
            throw new ClientExceptionHandler("Clinet Contact Update Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse> deleteClientContact(UUID id) {
        ClientEscalationContact Contact=clientContactRepo.findById(id).get();
        if(Contact!=null) {
            return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client Contact Deleted Successfully",Contact));
        }
        else {
            throw new ClientExceptionHandler("Clinet Contact Deletion Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getClientContact(UUID clientId) {
        List<ClientEscalationContact> Contact=clientContactRepo.findAllByClient_ClientId(clientId).orElseThrow(() -> new ClientExceptionHandler("Failed to fentch client Contact"));

        return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client Contact Fentched Successfully",Contact));

    }
}
