package com.service_imple.client_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.entity.client_entities.Client;
import com.entity.client_entities.ClientEscalationContact;
import com.repo.client_repo.ClientContactRepo;
import com.repo.client_repo.ClientRepo;
import com.service_interface.client_service_interface.ClientContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClientContactServiceImpl implements ClientContactService {

    private final ClientContactRepo clientContactRepo;
    private final ClientRepo clientRepo;

    @Override
    public ResponseEntity<ApiResponse<?>> createClientContact(ClientEscalationContact clientContact) {
        try {
            // Validate client exists
            Client client = clientRepo.findById(clientContact.getClient().getClientId())
                    .orElseThrow(() -> new RuntimeException("Client not found with ID: " + clientContact.getClient().getClientId()));

            // Check for duplicate email within the same client
            if (clientContactRepo.existsByEmailAndClient_ClientId(
                    clientContact.getEmail(), client.getClientId())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Email already exists for this client", null));
            }

            // Check for duplicate contact name within the same client
            if (clientContactRepo.existsByContactNameAndClient_ClientId(
                    clientContact.getContactName(), client.getClientId())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Contact name already exists for this client", null));
            }

            clientContact.setClient(client);

            ClientEscalationContact savedContact = clientContactRepo.save(clientContact);

            return ResponseEntity.ok(new ApiResponse<>(true, "Client contact created successfully", savedContact));

        } catch (RuntimeException e) {
            log.error("Error creating client contact: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error creating client contact: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to create client contact", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> updateClientContact(ClientEscalationContact clientContact) {
        try {
            // Validate contact exists
            ClientEscalationContact existingContact = clientContactRepo.findById(clientContact.getContactId())
                    .orElseThrow(() -> new RuntimeException("Client contact not found with ID: " + clientContact.getContactId()));

            // Validate client exists
            Client client = clientRepo.findById(clientContact.getClient().getClientId())
                    .orElseThrow(() -> new RuntimeException("Client not found with ID: " + clientContact.getClient().getClientId()));

            // Check for duplicate email (excluding current contact)
            if (clientContactRepo.findByEmailAndClientIdExcludingId(
                    clientContact.getEmail(), client.getClientId(), clientContact.getContactId()).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Email already exists for this client", null));
            }

            // Check for duplicate contact name (excluding current contact)
            if (clientContactRepo.findByContactNameAndClientIdExcludingId(
                    clientContact.getContactName(), client.getClientId(), clientContact.getContactId()).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Contact name already exists for this client", null));
            }

            // Update fields
            existingContact.setClient(client);
            existingContact.setContactName(clientContact.getContactName());
            existingContact.setContactRole(clientContact.getContactRole());
            existingContact.setEmail(clientContact.getEmail());
            existingContact.setPhone(clientContact.getPhone());
            existingContact.setActiveFlag(clientContact.getActiveFlag());
            existingContact.setEscalationLevel(clientContact.getEscalationLevel());

            ClientEscalationContact updatedContact = clientContactRepo.save(existingContact);

            return ResponseEntity.ok(new ApiResponse<>(true, "Client contact updated successfully", updatedContact));

        } catch (RuntimeException e) {
            log.error("Error updating client contact: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error updating client contact: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to update client contact", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> deleteClientContact(UUID id) {
        try {
            ClientEscalationContact existingContact = clientContactRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Client contact not found with ID: " + id));

            clientContactRepo.delete(existingContact);

            return ResponseEntity.ok(new ApiResponse<>(true, "Client contact deleted successfully", null));

        } catch (RuntimeException e) {
            log.error("Error deleting client contact: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error deleting client contact: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to delete client contact", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getClientContact(UUID clientId) {
        try {
            // Validate client exists
            if (!clientRepo.existsById(clientId)) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Client not found with ID: " + clientId, null));
            }

            List<ClientEscalationContact> contacts = clientContactRepo.findContactsByClientOrdered(clientId);

            return ResponseEntity.ok(new ApiResponse<>(true, "Client contacts retrieved successfully", contacts));

        } catch (Exception e) {
            log.error("Error retrieving client contacts: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to retrieve client contacts", null));
        }
    }
}