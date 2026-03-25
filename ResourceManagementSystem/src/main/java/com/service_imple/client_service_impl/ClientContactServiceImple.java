package com.service_imple.client_service_impl;


import com.dto.centralised_dto.ApiResponse;
import com.entity.client_entities.ClientEscalationContact;
import com.global_exception_handler.ClientExceptionHandler;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.client_repo.ClientContactRepo;
import com.service_interface.client_service_interface.ClientContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ApiResponse<?>> createClientContact(ClientEscalationContact clientContact) {
        // Validate required fields
        if (clientContact.getClient() == null || clientContact.getClient().getClientId() == null) {
            throw new ProjectExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "INVALID_CLIENT",
                "Client is required"
            );
        }
        
        // Check for duplicate email
        if (clientContact.getEmail() != null && 
            clientContactRepo.existsByEmailAndClient_ClientId(
                clientContact.getEmail(), 
                clientContact.getClient().getClientId())) {
            throw new ProjectExceptionHandler(
                HttpStatus.CONFLICT,
                "DUPLICATE_EMAIL",
                "Email already exists for this client"
            );
        }
        
        // Check for duplicate contact name
        if (clientContact.getContactName() != null && 
            clientContactRepo.existsByContactNameAndClient_ClientId(
                clientContact.getContactName(), 
                clientContact.getClient().getClientId())) {
            throw new ProjectExceptionHandler(
                HttpStatus.CONFLICT,
                "DUPLICATE_CONTACT_NAME",
                "Contact name already exists for this client"
            );
        }
        
        ClientEscalationContact Contact = clientContactRepo.save(clientContact);
        if(Contact != null) {
            return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client Contact Created Successfully",Contact));
        }
        else {
            throw new ClientExceptionHandler("Client Contact creation Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> updateClientContact(ClientEscalationContact clientContact) {
        // Validate contact exists
        if (clientContact.getContactId() == null) {
            throw new ProjectExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "INVALID_CONTACT_ID",
                "Contact ID is required for update"
            );
        }
        
        // Check for duplicate email (excluding current contact)
        if (clientContact.getEmail() != null && clientContact.getClient() != null) {
            var existingContact = clientContactRepo.findByEmailAndClientIdExcludingId(
                clientContact.getEmail(), 
                clientContact.getClient().getClientId(),
                clientContact.getContactId()
            );
            
            if (existingContact.isPresent()) {
                throw new ProjectExceptionHandler(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_EMAIL",
                    "Email already exists for this client"
                );
            }
        }
        
        // Check for duplicate contact name (excluding current contact)
        if (clientContact.getContactName() != null && clientContact.getClient() != null) {
            var existingContact = clientContactRepo.findByContactNameAndClientIdExcludingId(
                clientContact.getContactName(), 
                clientContact.getClient().getClientId(),
                clientContact.getContactId()
            );
            
            if (existingContact.isPresent()) {
                throw new ProjectExceptionHandler(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_CONTACT_NAME",
                    "Contact name already exists for this client"
                );
            }
        }
        
        ClientEscalationContact Contact = clientContactRepo.save(clientContact);
        if(Contact != null) {
            return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client Contact Updated Successfully",Contact));
        }
        else {
            throw new ClientExceptionHandler("Client Contact Update Failed");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> deleteClientContact(UUID id) {
        ClientEscalationContact Contact = clientContactRepo.findById(id).orElseThrow(() -> 
            new ProjectExceptionHandler(
                HttpStatus.NOT_FOUND,
                "CONTACT_NOT_FOUND",
                "Client Contact not found"
            )
        );
        
        clientContactRepo.delete(Contact);
        return ResponseEntity.ok(apiResponse.getAPIResponse(true,"Client Contact Deleted Successfully",Contact));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getClientContact(UUID clientId) {

        List<ClientEscalationContact> contact =
                clientContactRepo.findContactsByClientOrdered(clientId);

        if (contact.isEmpty()) {
            throw new ClientExceptionHandler("Failed to fetch client Contact");
        }

        return ResponseEntity.ok(
                apiResponse.getAPIResponse(
                        true,
                        "Client Contact Fetched Successfully",
                        contact
                )
        );
    }
}
