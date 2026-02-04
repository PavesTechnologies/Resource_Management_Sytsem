package com.service_interface.client_service_interface;

import com.dto.client_dto.ApiResponse;
import com.entity.client_entities.ClientEscalationContact;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface ClientContactService {
    ResponseEntity<ApiResponse> createClientContact(ClientEscalationContact clientContact);
    ResponseEntity<ApiResponse> updateClientContact(ClientEscalationContact clientContact);
    ResponseEntity<ApiResponse> deleteClientContact(UUID id);

    ResponseEntity<ApiResponse> getClientContact(UUID clientId);
}
