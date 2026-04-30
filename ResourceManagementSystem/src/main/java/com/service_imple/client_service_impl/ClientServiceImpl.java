package com.service_imple.client_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.dto.client_dto.*;
import com.entity.client_entities.Client;
import com.entity.project_entities.Project;
import com.entity_enums.centralised_enums.RecordStatus;
import com.entity_enums.project_enums.ProjectStatus;
import com.global_exception_handler.ClientExceptionHandler;
import com.repo.client_repo.ClientRepo;
import com.repo.project_repo.ProjectRepository;
import com.repo.project_repo.ProjectEscalationRepo;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
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
    private final ProjectRepository projectRepository;
    private final ProjectEscalationRepo projectEscalationRepo;

    @Override
    public ResponseEntity<ApiResponse<Client>> createClient(Client client) {
        try {
            if (client.getStatus() == null) {
                client.setStatus(RecordStatus.ACTIVE);
            }
            if (client.getCreatedAt() == null) {
                client.setCreatedAt(LocalDateTime.now());
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
            long count = clientRepo.count();
            return ResponseEntity.ok(ApiResponse.<Void>success("Client count retrieved successfully: " + count, null));

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
            
            Long activeProjectsCount = projectRepository.countByProjectStatus(ProjectStatus.ACTIVE);
            int activeProjects = activeProjectsCount != null ? activeProjectsCount.intValue() : 0;
            
            int currentYear = Year.now().getValue();
            int previousYear = currentYear - 1;
            
            long currentYearClientCount = getClientCountByYear(currentYear);
            long previousYearClientCount = getClientCountByYear(previousYear);
            
            // Growth percentage calculation: ((Current Year - Previous Year) / Previous Year) * 100
            // Rounding to int only as per requirement
            double growthPercentage = 0;
            if (previousYearClientCount > 0) {
                growthPercentage = Math.round(((double)(currentYearClientCount - previousYearClientCount) / previousYearClientCount) * 100);
            } else if (currentYearClientCount > 0) {
                growthPercentage = 100; // 100% growth if there were no clients in previous year but some in current year
            }
            
            Map<Integer, Long> yearlyClientCounts = new HashMap<>();
            yearlyClientCounts.put(currentYear - 2, getClientCountByYear(currentYear - 2));
            yearlyClientCounts.put(currentYear - 1, previousYearClientCount);
            yearlyClientCounts.put(currentYear, currentYearClientCount);
            
            AdminKPIDTO adminKPIDTO = AdminKPIDTO.builder()
                    .totalClients(totalClients)
                    .activeClients(activeClientCount)
                    .activeProjects(activeProjects)
                    .growthPercentage(growthPercentage)
                    .previousPeriodClientCount(previousYearClientCount)
                    .isGrowthPositive(growthPercentage >= 0)
                    .yearlyClientCounts(yearlyClientCounts)
                    .build();
            
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
                        Long totalProjects = projectRepository.countTotalProjectsByClientId(clientId);
                        Long activeProjects = projectRepository.countProjectsByClientIdAndStatus(clientId, ProjectStatus.ACTIVE);
                        BigDecimal totalSpend = projectRepository.sumProjectBudgetByClientId(clientId);
                        
                        // Enhanced Metrics Calculation
                        Long completedProjects = projectRepository.countCompletedProjectsByClientId(clientId);
                        Long onTimeCompleted = projectRepository.countOnTimeCompletedProjectsByClientId(clientId);
                        Double onTimeDeliveryRate = completedProjects > 0 ? (onTimeCompleted.doubleValue() / completedProjects.doubleValue()) * 100 : 0.0;
                        
                        Long delayedProjects = projectRepository.countDelayedProjectsByClientId(clientId);
                        Long highRiskProjects = projectRepository.countHighRiskProjectsByClientId(clientId);
                        Long readyProjects = projectRepository.countReadyProjectsByClientId(clientId);
                        
                        Double resourceReadiness = totalProjects > 0 ? (readyProjects.doubleValue() / totalProjects.doubleValue()) * 100 : 0.0;
                        
                        // Enhanced escalation metrics
                        Long activeEscalations = projectEscalationRepo.countActiveEscalationsByClientId(clientId);
                        Long highPriorityEscalations = projectEscalationRepo.countHighPriorityEscalationsByClientId(clientId);

                        // Satisfaction score derived from performance
                        Double satisfactionScore = 100.0;
                        if (totalProjects > 0) {
                            satisfactionScore = (onTimeDeliveryRate * 0.5) + (resourceReadiness * 0.3) + (100.0 - (highRiskProjects.doubleValue() / totalProjects * 100) * 0.2);
                        }

                        String overallHealth = "EXCELLENT";
                        if (highRiskProjects > 0 || delayedProjects > (totalProjects * 0.1) || activeEscalations > 2) {
                            overallHealth = "GOOD";
                        }
                        if (highRiskProjects > (totalProjects * 0.2) || delayedProjects > (totalProjects * 0.3) || highPriorityEscalations > 1) {
                            overallHealth = "FAIR";
                        }
                        if (highRiskProjects > (totalProjects * 0.4) || delayedProjects > (totalProjects * 0.5) || highPriorityEscalations > 3) {
                            overallHealth = "POOR";
                        }

                        ClientProjectStatisticsDTO.HealthMetrics healthMetrics = new ClientProjectStatisticsDTO.HealthMetrics(
                                onTimeDeliveryRate,
                                100.0, // Budget performance default (can be enhanced if actual spend vs budget is available)
                                resourceReadiness,
                                100.0 - (highRiskProjects.doubleValue() / (totalProjects > 0 ? totalProjects : 1) * 100),
                                highPriorityEscalations,
                                delayedProjects,
                                highRiskProjects
                        );

                        ClientProjectStatisticsDTO stats = new ClientProjectStatisticsDTO(
                                totalProjects,
                                activeProjects,
                                totalSpend,
                                Math.round(satisfactionScore * 10.0) / 10.0,
                                activeEscalations,
                                overallHealth,
                                healthMetrics
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
        Client clientDetails = clientRepo.findById(client.getClientId()).orElseThrow(() -> new RuntimeException("Client not found"));
        List<Project> project = projectRepository.findByClientId(clientDetails.getClientId());
        for (Project project1 : project) {
            if (project1.getProjectStatus().equals(ProjectStatus.ACTIVE)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Client>error("Client has active projects, cannot update", null));
            }
        }
        clientRepo.save(client);
        return ResponseEntity.ok(ApiResponse.<Client>success("Client updated successfully", client));
//        try {
//            return clientRepo.findById(client.getClientId())
//                    .map(existingClient -> {
//                        existingClient.setClientName(client.getClientName());
//                        existingClient.setClientType(client.getClientType());
//                        existingClient.setPriorityLevel(client.getPriorityLevel());
//                        existingClient.setDeliveryModel(client.getDeliveryModel());
//                        existingClient.setCountryName(client.getCountryName());
//                        existingClient.setDefaultTimezone(client.getDefaultTimezone());
//                        existingClient.setStatus(client.getStatus());
//                        existingClient.setSla(client.getSla());
//                        existingClient.setCompliance(client.getCompliance());
//                        existingClient.setEscalationContact(client.getEscalationContact());
//                        existingClient.setAssets(client.getAssets());
//                        existingClient.setUpdatedAt(LocalDateTime.now());
//
//                        Client updatedClient = clientRepo.save(existingClient);
//                        return ResponseEntity.ok(ApiResponse.<Client>success("Client updated successfully", updatedClient));
//                    })
//                    .orElse(ResponseEntity.badRequest()
//                            .body(ApiResponse.<Client>error("Client not found with ID: " + client.getClientId(), null)));
//
//        } catch (Exception e) {
//            log.error("Error updating client: {}", e.getMessage());
//            return ResponseEntity.internalServerError()
//                    .body(ApiResponse.<Client>error("Failed to update client", null));
//        }
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteClient(UUID id) {
        try {
            return clientRepo.findById(id)
                    .map(client -> {
                        // Check if client has associated projects
                        Long projectCount = projectRepository.countTotalProjectsByClientId(id);
                        if (projectCount > 0) {
                            return ResponseEntity.badRequest()
                                    .body(ApiResponse.<Void>error("Client has dependencies downstream. Cannot delete the Client.", null));
                        }
                        
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
    
    /**
     * Helper method to get client count by year
     */
    private long getClientCountByYear(int year) {
        try {
            return clientRepo.findAll().stream()
                    .filter(client -> client.getCreatedAt() != null && 
                            client.getCreatedAt().getYear() == year)
                    .count();
        } catch (Exception e) {
            log.error("Error getting client count for year {}: {}", year, e.getMessage());
            return 0;
        }
    }

    @Override
    public ResponseEntity<ApiResponse<List<ActiveClientsPMSDTO>>> getActiveClientsPMS() {
        List<ActiveClientsPMSDTO> clients = clientRepo.findByStatus(RecordStatus.ACTIVE).stream().map(c -> new ActiveClientsPMSDTO(c.getClientId(), c.getClientName())).toList();
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched Active Clients Successfully", clients));
    }
}
