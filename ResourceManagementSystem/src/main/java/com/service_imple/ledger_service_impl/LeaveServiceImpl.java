package com.service_imple.ledger_service_impl;

import com.service_interface.ledger_service_interface.LeaveService;
import com.dto.common.ApiHealthResponse;
import com.dto.common.ExternalLeaveDto;
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveServiceImpl implements LeaveService {

    private final RestTemplate restTemplate;
    
    @Value("${external.api.leave.base-url}")
    private String leaveApiBaseUrl;
    
    @Value("${external.api.leave.employee-endpoint}")
    private String leaveEmployeeEndpoint;

    private volatile boolean apiHealthy = true;
    private volatile long lastHealthCheck = 0;
    private static final long HEALTH_CHECK_INTERVAL_MS = 60000;

    @Override
    @Cacheable(value = "leaves", key = "#resourceId + '-' + #year", unless = "#result == null || #result.isEmpty()")
    @Retryable(value = {ResourceAccessException.class, HttpClientErrorException.class}, 
               maxAttempts = 3, backoff = @org.springframework.retry.annotation.Backoff(delay = 1000, multiplier = 2))
    public Set<LocalDate> getApprovedLeaveCached(Long resourceId, int year) throws LeaveApiException {
        return getApprovedLeaveInternal(resourceId, year);
    }

    @Override
    public Set<LocalDate> getApprovedLeaveForEmployee(Long resourceId, int year) throws LeaveApiException {
        try {
            return getApprovedLeaveCached(resourceId, year);
        } catch (Exception ex) {
            log.warn("Cache failure, falling back to API for leave resource {} year {}: {}", resourceId, year, ex.getMessage());
            return getApprovedLeaveInternal(resourceId, year);
        }
    }

    private Set<LocalDate> getApprovedLeaveInternal(Long resourceId, int year) throws LeaveApiException {
        try {
            checkApiHealth();
            
            String url = leaveApiBaseUrl + leaveEmployeeEndpoint.replace("{employeeId}", String.valueOf(resourceId)).replace("{year}", String.valueOf(year));
            LeaveApiResponse response = restTemplate.getForObject(url, LeaveApiResponse.class);
            
            if (response == null || response.getLeaves() == null) {
                return new HashSet<>();
            }

            Set<LocalDate> leaveDates = new HashSet<>();
            for (ExternalLeaveDto leave : response.getLeaves()) {
                if (leave.getLeaveDate() != null) {
                    leaveDates.add(leave.getLeaveDate());
                }
            }
            return leaveDates;

        } catch (ResourceAccessException e) {
            apiHealthy = false;
            log.error("Leave API connection failed for resource {} year {}: {}", resourceId, year, e.getMessage());
            throw new LeaveApiException("Leave API connection failed", e);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                return new HashSet<>();
            }
            apiHealthy = false;
            log.error("Leave API returned error for resource {} year {}: {}", resourceId, year, e.getMessage());
            throw new LeaveApiException("Leave API error: " + e.getMessage(), e);
        } catch (Exception e) {
            apiHealthy = false;
            log.error("Unexpected error fetching leave for resource {} year {}: {}", resourceId, year, e.getMessage());
            throw new LeaveApiException("Unexpected error", e);
        }
    }

    @Override
    public boolean isApiHealthy() {
        long now = System.currentTimeMillis();
        if (now - lastHealthCheck > HEALTH_CHECK_INTERVAL_MS) {
            performHealthCheck();
            lastHealthCheck = now;
        }
        return apiHealthy;
    }

    private void checkApiHealth() throws LeaveApiException {
        // Health check not implemented - API assumed to be available
        // Add health check implementation if needed in the future
    }

    private void performHealthCheck() {
        // Health check not implemented - API assumed to be healthy
        apiHealthy = true;
    }

    @Retryable(value = {ResourceAccessException.class}, maxAttempts = 2, backoff = @org.springframework.retry.annotation.Backoff(delay = 500))
    public CompletableFuture<Set<LocalDate>> getApprovedLeaveForEmployeeAsync(Long resourceId, int year) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getApprovedLeaveForEmployee(resourceId, year);
            } catch (LeaveApiException e) {
                return new HashSet<>();
            }
        });
    }

    public static class LeaveApiResponse {
        private Set<ExternalLeaveDto> leaves;
        private Long resourceId;
        private int year;
        private String lastUpdated;

        public Set<ExternalLeaveDto> getLeaves() { return leaves; }
        public void setLeaves(Set<ExternalLeaveDto> leaves) { this.leaves = leaves; }
        public Long getResourceId() { return resourceId; }
        public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        public String getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
    }
}
