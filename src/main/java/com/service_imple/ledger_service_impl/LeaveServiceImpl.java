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
            Set<LocalDate> leaveDates = parseApprovedLeaveDates(responseBody, resourceId);
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

    private Set<LocalDate> parseApprovedLeaveDates(String responseBody, String resourceId) throws Exception {
        Set<LocalDate> leaveDates = new HashSet<>();
        if (responseBody == null || responseBody.isBlank()) {
            return leaveDates;
        }

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode leaveDatesNode = resolveApprovedLeaveDatesNode(root, resourceId);
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

    // Resolves the array of leave date nodes from varying response shapes:
    // Shape A (standard): {"success":true, "data":[{"employeeId":"EMP001","approvedLeaveDates":["2025-01-15",...]}]}
    // Shape B (single obj): {"data":{"employeeId":"EMP001","approvedLeaveDates":[...]}}
    // Shape C (root array of date strings): ["2025-01-15",...]
    private JsonNode resolveApprovedLeaveDatesNode(JsonNode root, String resourceId) {
        if (root == null || root.isNull()) {
            return null;
        }

        // Shape C — root is a plain array of date strings
        if (root.isArray()) {
            return root;
        }

        JsonNode dataNode = root.get("data");
        if (dataNode != null && !dataNode.isNull()) {
            // Shape A — data is array of employee objects
            if (dataNode.isArray()) {
                // prefer the entry matching resourceId
                for (JsonNode emp : dataNode) {
                    JsonNode empId = emp.get("employeeId");
                    if (empId != null && resourceId.equals(empId.asText())) {
                        return emp.get("approvedLeaveDates");
                    }
                }
                // per-employee endpoint returns single-entry array — use it directly
                if (dataNode.size() == 1) {
                    JsonNode only = dataNode.get(0);
                    if (only.has("approvedLeaveDates")) {
                        return only.get("approvedLeaveDates");
                    }
                }
                return null;
            }
            // Shape B — data is a single employee object
            if (dataNode.has("approvedLeaveDates")) {
                return dataNode.get("approvedLeaveDates");
            }
            if (dataNode.has("leaves")) {
                return dataNode.get("leaves");
            }
        }

        // Legacy root-level fields
        if (root.has("approvedLeaveDates")) {
            return root.get("approvedLeaveDates");
        }
        if (root.has("leaves")) {
            return root.get("leaves");
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
