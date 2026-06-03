package com.service_imple.ledger_service_impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service_interface.ledger_service_interface.LeaveService;
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
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveServiceImpl implements LeaveService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${external.api.leave.base-url}")
    private String leaveApiBaseUrl;
    
    @Value("${external.api.leave.employee-endpoint}")
    private String leaveEmployeeEndpoint;

    private volatile boolean apiHealthy = true;

    @Override
    @Cacheable(value = "leaves", key = "#resourceId + '-' + #year")
    public Set<LocalDate> getApprovedLeaveForEmployee(String resourceId, int year) throws LeaveApiException {
        return getApprovedLeaveInternal(resourceId, year);
    }

    private Set<LocalDate> getApprovedLeaveInternal(String resourceId, int year) throws LeaveApiException {
        try {
            String url = leaveApiBaseUrl + leaveEmployeeEndpoint.replace("{employeeId}", resourceId).replace("{year}", String.valueOf(year));
            String responseBody = restTemplate.getForObject(url, String.class);
            Set<LocalDate> leaveDates = parseApprovedLeaveDates(responseBody);
            apiHealthy = true;
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

    private Set<LocalDate> parseApprovedLeaveDates(String responseBody) throws Exception {
        Set<LocalDate> leaveDates = new HashSet<>();
        if (responseBody == null || responseBody.isBlank()) {
            return leaveDates;
        }

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode leaveDatesNode = resolveApprovedLeaveDatesNode(root);
        if (leaveDatesNode == null || leaveDatesNode.isNull() || !leaveDatesNode.isArray()) {
            return leaveDates;
        }

        Iterator<JsonNode> iterator = leaveDatesNode.elements();
        while (iterator.hasNext()) {
            JsonNode leaveDateNode = iterator.next();
            if (leaveDateNode == null || leaveDateNode.isNull()) {
                continue;
            }

            if (leaveDateNode.isTextual()) {
                leaveDates.add(LocalDate.parse(leaveDateNode.asText()));
                continue;
            }

            JsonNode nestedDateNode = leaveDateNode.get("leaveDate");
            if (nestedDateNode != null && !nestedDateNode.isNull() && !nestedDateNode.asText().isBlank()) {
                leaveDates.add(LocalDate.parse(nestedDateNode.asText()));
            }
        }

        return leaveDates;
    }

    private JsonNode resolveApprovedLeaveDatesNode(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (root.has("leaves")) {
            return root.get("leaves");
        }
        JsonNode dataNode = root.get("data");
        if (dataNode == null || dataNode.isNull()) {
            return null;
        }
        if (dataNode.isArray()) {
            return dataNode;
        }
        if (dataNode.has("approvedLeaveDates")) {
            return dataNode.get("approvedLeaveDates");
        }
        if (dataNode.has("leaves")) {
            return dataNode.get("leaves");
        }
        return null;
    }

    @Override
    public boolean isApiHealthy() {
        return apiHealthy;
    }

    @Retryable(retryFor = {ResourceAccessException.class}, maxAttempts = 2, backoff = @org.springframework.retry.annotation.Backoff(delay = 500))
    public CompletableFuture<Set<LocalDate>> getApprovedLeaveForEmployeeAsync(String resourceId, int year) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getApprovedLeaveForEmployee(resourceId, year);
            } catch (LeaveApiException e) {
                return new HashSet<>();
            }
        });
    }

}
