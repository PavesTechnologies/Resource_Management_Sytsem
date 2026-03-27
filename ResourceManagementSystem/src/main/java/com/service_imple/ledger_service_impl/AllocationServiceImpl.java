package com.service_imple.ledger_service_impl;

import com.service_interface.ledger_service_interface.AllocationService;
import com.dto.common.ApiHealthResponse;
import com.dto.common.ExternalAllocationDto;
import com.dto.common.ExternalAllocationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

@Service("ledgerAllocationService") // Renamed bean
@RequiredArgsConstructor
@Slf4j
public class AllocationServiceImpl implements AllocationService {

    private final RestTemplate restTemplate;
    
    @Value("${allocation.api.base-url:http://localhost:8083/api/allocations}")
    private String allocationApiBaseUrl;

    private volatile boolean apiHealthy = true;
    private volatile long lastHealthCheck = 0;
    private static final long ALLOCATION_API_HEALTH_CHECK_INTERVAL_MS = 60000;

    @Override
    @Cacheable(value = "allocations", key = "#resourceId + '-' + #date", unless = "#result == null")
    @Retryable(value = {ResourceAccessException.class, HttpClientErrorException.class}, 
               maxAttempts = 3, backoff = @org.springframework.retry.annotation.Backoff(delay = 1000, multiplier = 2))
    public AllocationData getAllocationDataForResourceAndDate(Long resourceId, LocalDate date) {
        try {
            checkApiHealth();
            
            String url = String.format("%s/resources/%d/allocations?date=%s", 
                    allocationApiBaseUrl, resourceId, date);
            ExternalAllocationResponse response = restTemplate.getForObject(url, ExternalAllocationResponse.class);
            
            if (response == null || response.getAllocations() == null) {
                return new AllocationData(0, 0);
            }

            int confirmedPercentage = 0;
            int draftPercentage = 0;

            for (ExternalAllocationDto allocation : response.getAllocations()) {
                if (allocation.isConfirmed()) {
                    confirmedPercentage += allocation.getPercentage();
                } else if (allocation.isDraft()) {
                    draftPercentage += allocation.getPercentage();
                }
            }

            return new AllocationData(confirmedPercentage, draftPercentage);

        } catch (ResourceAccessException e) {
            apiHealthy = false;
            log.error("Allocation API connection failed for resource {} on date {}: {}", resourceId, date, e.getMessage());
            return new AllocationData(0, 0);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() != 404) {
                apiHealthy = false;
                log.error("Allocation API error for resource {} on date {}: {}", resourceId, date, e.getMessage());
            }
            return new AllocationData(0, 0);
        } catch (Exception e) {
            apiHealthy = false;
            log.error("Unexpected error fetching allocation for resource {} on date {}: {}", resourceId, date, e.getMessage());
            return new AllocationData(0, 0);
        }
    }

    @Override
    public boolean isApiHealthy() {
        long now = System.currentTimeMillis();
        if (now - lastHealthCheck > ALLOCATION_API_HEALTH_CHECK_INTERVAL_MS) {
            performHealthCheck();
            lastHealthCheck = now;
        }
        return apiHealthy;
    }

    private void checkApiHealth() {
        if (!apiHealthy) {
            performHealthCheck();
        }
    }

    private void performHealthCheck() {
        try {
            String healthUrl = allocationApiBaseUrl + "/health";
            ApiHealthResponse response = restTemplate.getForObject(healthUrl, ApiHealthResponse.class);
            apiHealthy = response != null && "UP".equals(response.getStatus());
        } catch (Exception e) {
            apiHealthy = false;
        }
    }

    @Retryable(value = {ResourceAccessException.class}, maxAttempts = 2, backoff = @org.springframework.retry.annotation.Backoff(delay = 500))
    public CompletableFuture<AllocationData> getAllocationDataForResourceAndDateAsync(Long resourceId, LocalDate date) {
        return CompletableFuture.supplyAsync(() -> getAllocationDataForResourceAndDate(resourceId, date));
    }

    @Override
    public LocalDate getMaxAllocationEndDate(Long resourceId) {
        try {
            String url = String.format("%s/resources/%d/allocations/max-end-date", allocationApiBaseUrl, resourceId);
            MaxEndDateResponse response = restTemplate.getForObject(url, MaxEndDateResponse.class);
            return (response != null) ? response.getMaxEndDate() : null;
        } catch (Exception e) {
            log.warn("Failed to get max allocation end date for resource {}: {}", resourceId, e.getMessage());
            return null;
        }
    }

    @Override
    public LocalDate getMaxAllocationEndDateAfter(Long resourceId, LocalDate baseDate) {
        try {
            String url = String.format("%s/resources/%d/allocations/max-end-date?after=%s", 
                    allocationApiBaseUrl, resourceId, baseDate);
            MaxEndDateResponse response = restTemplate.getForObject(url, MaxEndDateResponse.class);
            return (response != null) ? response.getMaxEndDate() : null;
        } catch (Exception e) {
            log.warn("Failed to get max end date after {} for resource {}: {}", baseDate, resourceId, e.getMessage());
            return null;
        }
    }

    @Override
    public LocalDate getResourceExitDate(Long resourceId) {
        try {
            String url = String.format("%s/resources/%d/exit-date", allocationApiBaseUrl, resourceId);
            ResourceExitDateResponse response = restTemplate.getForObject(url, ResourceExitDateResponse.class);
            return (response != null) ? response.getExitDate() : null;
        } catch (Exception e) {
            log.warn("Failed to get exit date for resource {}: {}", resourceId, e.getMessage());
            return null;
        }
    }

    public static class MaxEndDateResponse {
        private LocalDate maxEndDate;
        public LocalDate getMaxEndDate() { return maxEndDate; }
        public void setMaxEndDate(LocalDate maxEndDate) { this.maxEndDate = maxEndDate; }
    }

    public static class ResourceExitDateResponse {
        private LocalDate exitDate;
        public LocalDate getExitDate() { return exitDate; }
        public void setExitDate(LocalDate exitDate) { this.exitDate = exitDate; }
    }
}
