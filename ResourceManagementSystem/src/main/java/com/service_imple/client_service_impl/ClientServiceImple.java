package com.service_imple.client_service_impl;

import com.dto.client_dto.*;
import com.entity.client_entities.Client;
import com.entity_enums.centralised_enums.RecordStatus;
import com.global_exception_handler.ClientException;
import com.repo.client_repo.ClientRepo;
import com.service_interface.client_service_interface.ClientMapper;
import com.service_interface.client_service_interface.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import static org.springframework.util.StringUtils.hasText;


@Service
public class ClientServiceImple implements ClientService {

    @Autowired
    ClientRepo clientRepo;

    private final ClientMapper clientMapper;

    public ClientServiceImple(ClientMapper clientMapper) {
        this.clientMapper = clientMapper;
    }

    public class ClientSpecification {

        public static Specification<Client> build(ClientFilterDTO filter) {

            return (root, query, cb) -> {

                List<Predicate> predicates = new ArrayList<>();

                if (hasText(filter.getClientName())) {
                    predicates.add(cb.like(
                            cb.lower(root.get("clientName")),
                            "%" + filter.getClientName().toLowerCase() + "%"
                    ));
                }

                if (hasText(filter.getClientType())) {
                    predicates.add(cb.equal(root.get("clientType"), filter.getClientType()));
                }

                if (hasText(filter.getPriorityLevel())) {
                    predicates.add(cb.equal(root.get("priorityLevel"), filter.getPriorityLevel()));
                }

                if (hasText(filter.getCountryName())) {
                    predicates.add(cb.equal(root.get("countryName"), filter.getCountryName()));
                }

                if (hasText(filter.getDefaultTimezone())) {
                    predicates.add(cb.equal(root.get("defaultTimezone"), filter.getDefaultTimezone()));
                }

                if (hasText(filter.getStatus())) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }

                if (filter.getCreatedFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            root.get("createdAt"), filter.getCreatedFrom().atStartOfDay()));
                }

                if (filter.getCreatedTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            root.get("createdAt"), filter.getCreatedTo().atTime(23, 59, 59)));
                }

                // ✅ No predicates → return ALL records
                return cb.and(predicates.toArray(new Predicate[0]));
            };
        }

        private static boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }


    @Override
    public ResponseEntity<ApiResponse> createClient(Client client) {
        try {
            // Simple regex validation for client name
            if (client.getClientName() == null || client.getClientName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Client name is required", null));
            }
            
            if (!client.getClientName().matches("^[A-Za-z]+(?:[ .][A-Za-z]+)*$")) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Client name can contain only letters, spaces, and dots", null));
            }
            
            if (client.getClientName().length() < 3 || client.getClientName().length() > 100) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Client name must be between 3 and 100 characters", null));
            }
            
            client.setCreatedAt(LocalDateTime.now());
            client.setUpdatedAt(LocalDateTime.now());
            Client c = clientRepo.save(client);

            ApiResponse<Client> apiResponse = new ApiResponse<>();
            apiResponse.setSuccess(c!=null);
            apiResponse.setMessage(c != null
                    ? "Client Created Successfully"
                    : "Client Creation Failed");
            apiResponse.setData(c);

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Error creating client: " + e.getMessage(), null));
        }
    }

    @Override
    public ApiResponse<PageResponse<ClientDTO>> searchClients(
            ClientFilterDTO filter,
            int page,
            int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

            // Build specification from filter
            Specification<Client> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (hasText(filter.getClientName())) {
                    predicates.add(cb.like(
                            cb.lower(root.get("clientName")),
                            "%" + filter.getClientName().toLowerCase() + "%"
                    ));
                }

                if (hasText(filter.getClientType())) {
                    predicates.add(cb.equal(root.get("clientType"), filter.getClientType()));
                }

                if (hasText(filter.getPriorityLevel())) {
                    predicates.add(cb.equal(root.get("priorityLevel"), filter.getPriorityLevel()));
                }

                if (hasText(filter.getDeliveryModel())) {
                    predicates.add(cb.equal(root.get("deliveryModel"), filter.getDeliveryModel()));
                }

                if (hasText(filter.getCountryName())) {
                    predicates.add(cb.equal(root.get("countryName"), filter.getCountryName()));
                }

                if (hasText(filter.getStatus())) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }

                if (filter.getCreatedFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            root.get("createdAt"), filter.getCreatedFrom().atStartOfDay()));
                }

                if (filter.getCreatedTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            root.get("createdAt"), filter.getCreatedTo().atTime(23, 59, 59)));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Page<Client> result = clientRepo.findAll(spec, pageable);

            // No records found
            if (result.isEmpty()) {
                ApiResponse<PageResponse<ClientDTO>> response = new ApiResponse<>();
                response.setSuccess(false);
                response.setMessage("No clients found for the given filters");
                response.setData(null);
                return response;
            }

            // Records found
            PageResponse<ClientDTO> pageResponse = new PageResponse<>(
                    result.getContent()
                            .stream()
                            .map(clientMapper::toDto)
                            .toList(),
                    result.getNumber(),
                    result.getSize(),
                    result.getTotalElements(),
                    result.getTotalPages()
            );
            ApiResponse<PageResponse<ClientDTO>> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage("Records Fentched Successfully");
            response.setData(pageResponse);
            return response;
        } catch (Exception e) {
            ApiResponse<PageResponse<ClientDTO>> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage("Error fetching clients");
            response.setData(null);
            return response;
        }
    }

    @Override
    public ResponseEntity<ApiResponse> countClients() {
        clientRepo.findAll().size();
        return null;
    }

    @Override
    public ResponseEntity<ApiResponse<List<Client>>> clientDetails() {
        List<Client> clients = clientRepo.findByStatus(RecordStatus.ACTIVE);
        return ResponseEntity.ok(new ApiResponse<>(true, "Clients fetched successfully", clients));
    }

    @Override
    public ResponseEntity<ApiResponse<AdminKPIDTO>> getAdminKPI() {
        try {
            // Get current year and previous year
            int currentYear = java.time.Year.now().getValue();
            int previousYear = currentYear - 1;
            
            // Get all clients
            List<Client> allClients = clientRepo.findAll();
            
            // Get active clients
            List<Client> activeClients = clientRepo.findByStatus(RecordStatus.ACTIVE);
            
            // Calculate total clients
            int totalClients = allClients.size();
            
            // Calculate active clients
            int activeClientsCount = activeClients.size();
            
            // Get clients created in current year and previous year for growth calculation
            List<Client> currentYearClients = allClients.stream()
                .filter(client -> client.getCreatedAt() != null && 
                               client.getCreatedAt().getYear() == currentYear)
                .toList();
            
            List<Client> previousYearClients = allClients.stream()
                .filter(client -> client.getCreatedAt() != null && 
                               client.getCreatedAt().getYear() == previousYear)
                .toList();
            
            int currentYearClientCount = currentYearClients.size();
            int previousYearClientCount = previousYearClients.size();
            
            // Calculate growth percentage
            double growthPercentage = 0.0;
            boolean isGrowthPositive = false;
            
            if (previousYearClientCount > 0) {
                growthPercentage = ((double)(currentYearClientCount - previousYearClientCount) / previousYearClientCount) * 100;
                isGrowthPositive = growthPercentage >= 0;
            } else if (currentYearClientCount > 0) {
                growthPercentage = 100.0; // 100% growth if no previous year clients but have current year clients
                isGrowthPositive = true;
            }
            
            // Create yearly client counts map for UI chart
            Map<Integer, Integer> yearlyClientCounts = Map.of(
                previousYear, previousYearClientCount,
                currentYear, currentYearClientCount
            );
            
            // Create KPI DTO
            AdminKPIDTO adminKPIDTO = new AdminKPIDTO();
            adminKPIDTO.setTotalClients(totalClients);
            adminKPIDTO.setActiveClients(activeClientsCount);
            adminKPIDTO.setActiveProjects(0); // No project entity found, set to 0
            adminKPIDTO.setGrowthPercentage(Math.round(growthPercentage * 100.0) / 100.0); // Round to 2 decimal places
            adminKPIDTO.setPreviousPeriodClientCount(previousYearClientCount);
            adminKPIDTO.setGrowthPositive(isGrowthPositive);
            adminKPIDTO.setYearlyClientCounts(yearlyClientCounts);
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Admin KPI data fetched successfully", adminKPIDTO));
            
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse<>(false, "Error fetching admin KPI data: " + e.getMessage(), null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<Client>> getClientById(UUID id) {
        Client client = clientRepo.findById(id).orElseThrow(() -> new ClientException("Client not found"));
        return ResponseEntity.ok(new ApiResponse<>(true, "Client fetched successfully", client));
    }

    @Override
    public ResponseEntity<ApiResponse<Client>> updateClient(Client client) {
        try {
            // Simple regex validation for client name
            if (client.getClientName() == null || client.getClientName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Client name is required", null));
            }
            
            if (!client.getClientName().matches("^[A-Za-z]+(?:[ .][A-Za-z]+)*$")) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Client name can contain only letters, spaces, and dots", null));
            }
            
            if (client.getClientName().length() < 3 || client.getClientName().length() > 100) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Client name must be between 3 and 100 characters", null));
            }
            
            Client clientDetails = clientRepo.findById(client.getClientId()).orElseThrow(() -> new ClientException("Client Not Found!"));
            client.setCreatedAt(clientDetails.getCreatedAt());
            client.setUpdatedAt(LocalDateTime.now());
            Client updatedDetails = clientRepo.save(client);
            return ResponseEntity.ok(new ApiResponse<>(true, "Client Details Updated.", updatedDetails));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Error updating client: " + e.getMessage(), null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> deleteClient(UUID id) {
        Client client = clientRepo.findById(id).orElseThrow(() -> new ClientException("Client Not Found!"));
        client.setStatus(RecordStatus.INACTIVE);
        client.setUpdatedAt(LocalDateTime.now());
        clientRepo.save(client);
        return ResponseEntity.ok(new ApiResponse(true, "Client Deleted Successfully!", null));
    }
}

