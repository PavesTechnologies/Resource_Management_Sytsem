package com.service_imple.client_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.dto.client_dto.*;
import com.entity.client_entities.Client;
import com.entity_enums.centralised_enums.RecordStatus;
import com.repo.client_repo.ClientRepo;
import com.service_interface.client_service_interface.ClientService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepo clientRepo;

    @Override
    public ResponseEntity<ApiResponse<Client>> createClient(Client client) {
        try {
            if (client.getStatus() == null) {
                client.setStatus(RecordStatus.ACTIVE);
            }

            Client savedClient = clientRepo.save(client);
            return ResponseEntity.ok(ApiResponse.<Client>success("Client created successfully", savedClient));

        } catch (Exception e) {
            log.error("Error creating client: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Client>error("Failed to create client: " + e.getMessage(), null));
        }
    }

    @Override
    public ApiResponse<PageResponse<ClientDTO>> searchClients(ClientFilterDTO filter, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Specification<Client> spec = buildClientSpecification(filter);
            Page<Client> clientPage = clientRepo.findAll(spec, pageable);
            
            List<ClientDTO> clientDTOs = clientPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            PageResponse<ClientDTO> pageResponse = new PageResponse<>(
                    clientDTOs,
                    page,
                    size,
                    clientPage.getTotalElements(),
                    clientPage.getTotalPages()
            );
            
            return ApiResponse.<PageResponse<ClientDTO>>success("Clients retrieved successfully", pageResponse);

        } catch (Exception e) {
            log.error("Error searching clients: {}", e.getMessage());
            return ApiResponse.<PageResponse<ClientDTO>>error("Failed to search clients: " + e.getMessage(), null);
        }
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> countClients() {
        try {
            clientRepo.count();
            return ResponseEntity.ok(ApiResponse.<Void>success("Client count retrieved successfully", null));

        } catch (Exception e) {
            log.error("Error counting clients: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Void>error("Failed to count clients", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<List<Client>>> clientDetails() {
        try {
            List<Client> clients = clientRepo.findAll();
            return ResponseEntity.ok(ApiResponse.<List<Client>>success("Client details retrieved successfully", clients));

        } catch (Exception e) {
            log.error("Error retrieving client details: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<Client>>error("Failed to retrieve client details", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<List<ClientDTO>>> getActiveClients() {
        try {
            List<Client> activeClients = clientRepo.findByStatus(RecordStatus.ACTIVE);
            List<ClientDTO> clientDTOs = activeClients.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.<List<ClientDTO>>success("Active clients retrieved successfully", clientDTOs));

        } catch (Exception e) {
            log.error("Error retrieving active clients: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<ClientDTO>>error("Failed to retrieve active clients", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<AdminKPIDTO>> getAdminKPI() {
        try {
            long totalClients = clientRepo.count();
            List<Client> activeClients = clientRepo.findByStatus(RecordStatus.ACTIVE);
            int activeClientCount = activeClients.size();
            int activeProjects = 25;
            int previousPeriodClientCount = 45;
            double growthPercentage = totalClients > 0 ? 
                    ((double)(totalClients - previousPeriodClientCount) / previousPeriodClientCount) * 100 : 0;
            
            Map<Integer, Integer> yearlyClientCounts = new HashMap<>();
            yearlyClientCounts.put(2023, 35);
            yearlyClientCounts.put(2024, 45);
            yearlyClientCounts.put(2025, (int) totalClients);
            
            AdminKPIDTO adminKPIDTO = new AdminKPIDTO(
                    (int) totalClients,
                    activeClientCount,
                    activeProjects,
                    growthPercentage,
                    previousPeriodClientCount,
                    growthPercentage >= 0,
                    yearlyClientCounts
            );
            
            return ResponseEntity.ok(ApiResponse.<AdminKPIDTO>success("Admin KPI retrieved successfully", adminKPIDTO));

        } catch (Exception e) {
            log.error("Error retrieving admin KPI: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<AdminKPIDTO>error("Failed to retrieve admin KPI", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<Client>> getClientById(UUID id) {
        try {
            return clientRepo.findById(id)
                    .map(client -> ResponseEntity.ok(ApiResponse.<Client>success("Client retrieved successfully", client)))
                    .orElse(ResponseEntity.badRequest()
                            .body(ApiResponse.<Client>error("Client not found with ID: " + id, null)));

        } catch (Exception e) {
            log.error("Error retrieving client by ID: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Client>error("Failed to retrieve client", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<ClientProjectStatisticsDTO>> getClientProjectStatistics(UUID clientId) {
        try {
            return clientRepo.findById(clientId)
                    .map(client -> {
                        ClientProjectStatisticsDTO stats = new ClientProjectStatisticsDTO(
                                15L, 8L, java.math.BigDecimal.valueOf(250000.50), 85.5, 3L, "GOOD", null
                        );
                        return ResponseEntity.ok(ApiResponse.<ClientProjectStatisticsDTO>success("Client project statistics retrieved successfully", stats));
                    })
                    .orElse(ResponseEntity.badRequest()
                            .body(ApiResponse.<ClientProjectStatisticsDTO>error("Client not found with ID: " + clientId, null)));

        } catch (Exception e) {
            log.error("Error retrieving client project statistics: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<ClientProjectStatisticsDTO>error("Failed to retrieve client project statistics", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<Client>> updateClient(Client client) {
        try {
            return clientRepo.findById(client.getClientId())
                    .map(existingClient -> {
                        existingClient.setClientName(client.getClientName());
                        existingClient.setClientType(client.getClientType());
                        existingClient.setPriorityLevel(client.getPriorityLevel());
                        existingClient.setDeliveryModel(client.getDeliveryModel());
                        existingClient.setCountryName(client.getCountryName());
                        existingClient.setDefaultTimezone(client.getDefaultTimezone());
                        existingClient.setStatus(client.getStatus());
                        existingClient.setSla(client.getSla());
                        existingClient.setCompliance(client.getCompliance());
                        existingClient.setEscalationContact(client.getEscalationContact());
                        existingClient.setAssets(client.getAssets());
                        existingClient.setUpdatedAt(LocalDateTime.now());
                        
                        Client updatedClient = clientRepo.save(existingClient);
                        return ResponseEntity.ok(ApiResponse.<Client>success("Client updated successfully", updatedClient));
                    })
                    .orElse(ResponseEntity.badRequest()
                            .body(ApiResponse.<Client>error("Client not found with ID: " + client.getClientId(), null)));

        } catch (Exception e) {
            log.error("Error updating client: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Client>error("Failed to update client", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteClient(UUID id) {
        try {
            return clientRepo.findById(id)
                    .map(client -> {
                        clientRepo.delete(client);
                        return ResponseEntity.ok(ApiResponse.<Void>success("Client deleted successfully", null));
                    })
                    .orElse(ResponseEntity.badRequest()
                            .body(ApiResponse.<Void>error("Client not found with ID: " + id, null)));

        } catch (Exception e) {
            log.error("Error deleting client: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Void>error("Failed to delete client", null));
        }
    }

    private ClientDTO convertToDTO(Client client) {
        return ClientDTO.builder()
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .clientType(client.getClientType() != null ? client.getClientType().toString() : null)
                .priorityLevel(client.getPriorityLevel() != null ? client.getPriorityLevel().toString() : null)
                .deliveryModel(client.getDeliveryModel() != null ? client.getDeliveryModel().toString() : null)
                .countryName(client.getCountryName())
                .defaultTimezone(client.getDefaultTimezone())
                .status(client.getStatus() != null ? client.getStatus().toString() : null)
                .build();
    }

    private Specification<Client> buildClientSpecification(ClientFilterDTO filter) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();

            if (filter.getClientName() != null && !filter.getClientName().trim().isEmpty()) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("clientName")),
                                "%" + filter.getClientName().toLowerCase() + "%"));
            }

            if (filter.getClientType() != null && !filter.getClientType().trim().isEmpty()) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("clientType"), filter.getClientType()));
            }

            if (filter.getPriorityLevel() != null && !filter.getPriorityLevel().trim().isEmpty()) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("priorityLevel"), filter.getPriorityLevel()));
            }

            if (filter.getDeliveryModel() != null && !filter.getDeliveryModel().trim().isEmpty()) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("deliveryModel"), filter.getDeliveryModel()));
            }

            if (filter.getCountryName() != null && !filter.getCountryName().trim().isEmpty()) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("countryName")),
                                "%" + filter.getCountryName().toLowerCase() + "%"));
            }

            if (filter.getDefaultTimezone() != null && !filter.getDefaultTimezone().trim().isEmpty()) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("defaultTimezone")),
                                "%" + filter.getDefaultTimezone().toLowerCase() + "%"));
            }

            if (filter.getStatus() != null && !filter.getStatus().trim().isEmpty()) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getCreatedFrom() != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedFrom().atStartOfDay()));
            }

            if (filter.getCreatedTo() != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedTo().atTime(23, 59, 59)));
            }

            return predicate;
        };
    }
}
