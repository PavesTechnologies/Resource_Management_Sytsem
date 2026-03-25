package com.service_imple.client_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.dto.client_dto.ClientProjectStatisticsDTO;
import com.dto.client_dto.*;
import com.entity.client_entities.Client;
import com.entity_enums.project_enums.ProjectStatus;
import com.entity_enums.centralised_enums.RecordStatus;
import com.global_exception_handler.ClientExceptionHandler;
import com.repo.project_repo.ProjectRepository;
import com.repo.client_repo.ClientRepo;
import com.repo.allocation_repo.AllocationRepository;
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

import java.math.BigDecimal;
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

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    AllocationRepository allocationRepository;

    @Autowired
    com.repo.project_repo.ProjectEscalationRepo projectEscalationRepo;

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
    public ResponseEntity<ApiResponse<Client>> createClient(Client client) {
        try {
            // Simple regex validation for client name
            if (client.getClientName() == null || client.getClientName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Client name is required"));
            }
            
            if (!client.getClientName().matches("^[A-Za-z]+(?:[ .][A-Za-z]+)*$")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Client name can contain only letters, spaces, and dots"));
            }
            
            if (client.getClientName().length() < 3 || client.getClientName().length() > 100) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Client name must be between 3 and 100 characters"));
            }
            
            client.setCreatedAt(LocalDateTime.now());
            client.setUpdatedAt(LocalDateTime.now());
            Client c = clientRepo.save(client);

            ApiResponse<Client> apiResponse = ApiResponse.success(
                    c != null ? "Client Created Successfully" : "Client Creation Failed",
                    c
            );

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Error creating client: " + e.getMessage()));
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
                return ApiResponse.error("No clients found for the given filters");
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
            return ApiResponse.success("Records Fetched Successfully", pageResponse);
        } catch (Exception e) {
            return ApiResponse.error("Error fetching clients");
        }
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> countClients() {
        clientRepo.findAll().size();
        return null;
    }

    @Override
    public ResponseEntity<ApiResponse<List<Client>>> clientDetails() {
        List<Client> clients = clientRepo.findByStatus(RecordStatus.ACTIVE);
        return ResponseEntity.ok(new ApiResponse<>(true, "Clients fetched successfully", clients));
    }

    @Override
    public ResponseEntity<ApiResponse<List<ClientDTO>>> getActiveClients() {
        try {
            List<Client> activeClients = clientRepo.findByStatus(RecordStatus.ACTIVE);
            List<ClientDTO> clientDTOs = activeClients.stream()
                    .map(clientMapper::toDto)
                    .toList();
            return ResponseEntity.ok(ApiResponse.success("Active clients fetched successfully", clientDTOs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Error fetching active clients: " + e.getMessage()));
        }
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
            // Calculate active projects count (ACTIVE, APPROVED, PLANNING)
            List<ProjectStatus> activeStatuses = List.of(ProjectStatus.ACTIVE, ProjectStatus.APPROVED, ProjectStatus.PLANNING);
            long activeProjectsCount = projectRepository.countByProjectStatuses(activeStatuses);
            adminKPIDTO.setActiveProjects((int) activeProjectsCount);
            adminKPIDTO.setGrowthPercentage(Math.round(growthPercentage * 100.0) / 100.0); // Round to 2 decimal places
            adminKPIDTO.setPreviousPeriodClientCount(previousYearClientCount);
            adminKPIDTO.setGrowthPositive(isGrowthPositive);
            adminKPIDTO.setYearlyClientCounts(yearlyClientCounts);
            
            return ResponseEntity.ok(ApiResponse.<AdminKPIDTO>success("Admin KPI data fetched successfully", adminKPIDTO));
            
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Error fetching admin KPI data: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<Client>> getClientById(UUID id) {
        Client client = clientRepo.findById(id).orElseThrow(() -> new ClientExceptionHandler("Client not found"));
        return ResponseEntity.ok(ApiResponse.<Client>success("Client fetched successfully", client));
    }

    @Override
    public ResponseEntity<ApiResponse<Client>> updateClient(Client client) {
        try {
            // Simple regex validation for client name
            if (client.getClientName() == null || client.getClientName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Client name is required"));
            }
            
            if (!client.getClientName().matches("^[A-Za-z]+(?:[ .][A-Za-z]+)*$")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Client name can contain only letters, spaces, and dots"));
            }
            
            if (client.getClientName().length() < 3 || client.getClientName().length() > 100) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Client name must be between 3 and 100 characters"));
            }
            
            Client clientDetails = clientRepo.findById(client.getClientId())
                .orElseThrow(() -> new ClientExceptionHandler("Client Not Found!"));
            
            // Check if status is being changed
            if (client.getStatus() != null && !client.getStatus().equals(clientDetails.getStatus())) {
                // Check for active projects when trying to change to INACTIVE or ON_HOLD
                if (client.getStatus() == RecordStatus.INACTIVE || client.getStatus() == RecordStatus.ON_HOLD) {
                    boolean hasActiveProjects = projectRepository.existsByClientIdAndProjectStatus(client.getClientId(), ProjectStatus.ACTIVE);
                    boolean hasActiveAllocations = allocationRepository.existsByClientIdAndActiveAllocation(client.getClientId());
                    
                    if (hasActiveProjects && hasActiveAllocations) {
                        return ResponseEntity.badRequest().body(ApiResponse.error("Cannot change client status to " + client.getStatus() + ". Client has active projects and active resource allocations. Please complete or reassign all projects and allocations before changing status."));
                    } else if (hasActiveProjects) {
                        return ResponseEntity.badRequest().body(ApiResponse.error("Cannot change client status to " + client.getStatus() + ". Client has active projects. Please complete or reassign all projects before changing status."));
                    } else if (hasActiveAllocations) {
                        return ResponseEntity.badRequest().body(ApiResponse.error("Cannot change client status to " + client.getStatus() + ". Client has active resource allocations. Please reassign or release all allocations before changing status."));
                    }
                }
            }
            
            client.setCreatedAt(clientDetails.getCreatedAt());
            client.setUpdatedAt(LocalDateTime.now());
            Client updatedDetails = clientRepo.save(client);
            return ResponseEntity.ok(ApiResponse.<Client>success("Client Details Updated.", updatedDetails));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Error updating client: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<ClientProjectStatisticsDTO>> getClientProjectStatistics(UUID clientId) {
        try {
            // Verify client exists
            Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new ClientExceptionHandler("Client not found"));
            
            // Get basic project statistics
            Long totalProjects = projectRepository.countTotalProjectsByClientId(clientId);
            Long activeProjects = projectRepository.countProjectsByClientIdAndStatus(clientId, ProjectStatus.ACTIVE);
            BigDecimal totalSpend = projectRepository.sumProjectBudgetByClientId(clientId);
            
            // Calculate enhanced metrics
            ClientProjectStatisticsDTO.HealthMetrics healthMetrics = calculateHealthMetrics(clientId, totalProjects, activeProjects);
            Double satisfactionScore = calculateSatisfactionScore(healthMetrics);
            String overallHealth = determineOverallHealth(satisfactionScore);
            Long pendingIssues = projectEscalationRepo.countActiveEscalationsByClientId(clientId);
            
            // Create and populate the enhanced DTO
            ClientProjectStatisticsDTO statistics = new ClientProjectStatisticsDTO();
            statistics.setTotalProjects(totalProjects);
            statistics.setActiveProjects(activeProjects);
            statistics.setTotalSpend(totalSpend);
            statistics.setSatisfactionScore(satisfactionScore);
            statistics.setPendingIssues(pendingIssues);
            statistics.setOverallHealth(overallHealth);
            statistics.setHealthMetrics(healthMetrics);
            
            return ResponseEntity.ok(ApiResponse.<ClientProjectStatisticsDTO>success("Client project statistics fetched successfully", statistics));
            
        } catch (ClientExceptionHandler e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Error fetching client project statistics: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteClient(UUID id) {
        Client client = clientRepo.findById(id).orElseThrow(() -> new ClientExceptionHandler("Client Not Found!"));

        // Check for active projects
        boolean hasActiveProjects = projectRepository.existsByClientIdAndProjectStatus(id, ProjectStatus.ACTIVE);
        
        // Check for active allocations
        boolean hasActiveAllocations = allocationRepository.existsByClientIdAndActiveAllocation(id);
        
        if (hasActiveProjects && hasActiveAllocations) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Cannot deactivate client. Client has active projects and active resource allocations. Please complete or reassign all projects and allocations before deactivation.", null));
        } else if (hasActiveProjects) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Cannot deactivate client. Client has active projects. Please complete or reassign all projects before deactivation.", null));
        } else if (hasActiveAllocations) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Cannot deactivate client. Client has active resource allocations. Please reassign or release all allocations before deactivation.", null));
        }
        
        client.setStatus(RecordStatus.INACTIVE);
        client.setUpdatedAt(LocalDateTime.now());
        clientRepo.save(client);
        return ResponseEntity.ok(ApiResponse.success("Client deactivated successfully!", null));
    }

    /**
     * Calculate detailed health metrics for a client
     */
    private ClientProjectStatisticsDTO.HealthMetrics calculateHealthMetrics(UUID clientId, Long totalProjects, Long activeProjects) {
        ClientProjectStatisticsDTO.HealthMetrics metrics = new ClientProjectStatisticsDTO.HealthMetrics();
        
        if (totalProjects == null || totalProjects == 0) {
            // Return zero metrics if no projects
            metrics.setOnTimeDeliveryRate(0.0);
            metrics.setBudgetPerformance(0.0);
            metrics.setResourceReadiness(0.0);
            metrics.setRiskManagement(0.0);
            metrics.setHighPriorityEscalations(0L);
            metrics.setDelayedProjects(0L);
            metrics.setHighRiskProjects(0L);
            return metrics;
        }

        // On-time delivery rate
        Long onTimeCompleted = projectRepository.countOnTimeCompletedProjectsByClientId(clientId);
        Long completed = projectRepository.countCompletedProjectsByClientId(clientId);
        metrics.setOnTimeDeliveryRate(completed > 0 ? (double) onTimeCompleted / completed * 100 : 0.0);

        // Budget performance (assuming all projects are within budget for now - can be enhanced with actual cost tracking)
        metrics.setBudgetPerformance(85.0); // Default assumption - can be enhanced with real budget variance data

        // Resource readiness
        Long readyProjects = projectRepository.countReadyProjectsByClientId(clientId);
        metrics.setResourceReadiness(activeProjects > 0 ? (double) readyProjects / activeProjects * 100 : 0.0);

        // Risk management (percentage of projects with low/medium risk)
        Long lowMediumRiskProjects = projectRepository.countLowMediumRiskProjectsByClientId(clientId);
        metrics.setRiskManagement(totalProjects > 0 ? (double) lowMediumRiskProjects / totalProjects * 100 : 0.0);

        // Issue counts
        metrics.setHighPriorityEscalations(projectEscalationRepo.countHighPriorityEscalationsByClientId(clientId));
        metrics.setDelayedProjects(projectRepository.countDelayedProjectsByClientId(clientId));
        metrics.setHighRiskProjects(projectRepository.countHighRiskProjectsByClientId(clientId));

        return metrics;
    }

    /**
     * Calculate overall satisfaction score based on health metrics
     */
    private Double calculateSatisfactionScore(ClientProjectStatisticsDTO.HealthMetrics metrics) {
        if (metrics == null) {
            return 0.0;
        }

        // Weighted calculation: On-time delivery (40%) + Budget performance (40%) + Low escalation rate (20%)
        double onTimeScore = metrics.getOnTimeDeliveryRate() != null ? metrics.getOnTimeDeliveryRate() : 0.0;
        double budgetScore = metrics.getBudgetPerformance() != null ? metrics.getBudgetPerformance() : 0.0;
        
        // Escalation penalty: reduce score based on high priority escalations
        double escalationPenalty = Math.min(20.0, metrics.getHighPriorityEscalations() * 5.0);
        double escalationScore = Math.max(0.0, 20.0 - escalationPenalty);

        return Math.round((onTimeScore * 0.4 + budgetScore * 0.4 + escalationScore) * 100.0) / 100.0;
    }

    /**
     * Determine overall health category based on satisfaction score
     */
    private String determineOverallHealth(Double satisfactionScore) {
        if (satisfactionScore == null) {
            return "POOR";
        }
        
        if (satisfactionScore >= 90) {
            return "EXCELLENT";
        } else if (satisfactionScore >= 75) {
            return "GOOD";
        } else if (satisfactionScore >= 60) {
            return "FAIR";
        } else {
            return "POOR";
        }
    }
}
