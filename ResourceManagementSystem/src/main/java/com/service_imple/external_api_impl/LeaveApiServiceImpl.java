package com.service_imple.external_api_impl;

import com.dto.external.LeaveApiResponse;
import com.service.TokenService;
import com.service_interface.external_api_interface.LeaveApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class LeaveApiServiceImpl implements LeaveApiService {

    private final RestTemplate restTemplate;
    private final TokenService tokenService;

    @Value("${external.api.leave.base-url:http://16.16.202.195:9999}")
    private String leaveApiBaseUrl;

    public LeaveApiServiceImpl(RestTemplate restTemplate, TokenService tokenService) {
        this.restTemplate = restTemplate;
        this.tokenService = tokenService;
    }

    @Override
    public LeaveApiResponse getApprovedLeaveForYear(Integer year) throws LeaveApiService.ExternalApiException {
        try {
            String url = leaveApiBaseUrl + "/api/leave-requests/approved/" + year;
            log.info("Fetching approved leave from: {}", url);
            
            // Token is added by AvailabilityEngineConfig interceptor
            HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

            ResponseEntity<LeaveApiResponse> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                LeaveApiResponse.class
            );
            
            LeaveApiResponse responseBody = response.getBody();
            
            if (responseBody == null || !responseBody.isSuccess()) {
                log.warn("Leave API returned unsuccessful response for year: {}", year);
                return new LeaveApiResponse();
            }
            
            log.info("Successfully fetched leave data for year: {}", year);
            return responseBody;
            
        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching approved leave for year: {} - Status: {}, Message: {}", year, e.getStatusCode(), e.getMessage());
            
            if (e.getStatusCode().value() == 401) {
                tokenService.invalidateToken();
                throw new LeaveApiService.ExternalApiException("Authentication failed with leave API. Token has been invalidated.", e);
            } else if (e.getStatusCode().value() == 403) {
                throw new LeaveApiService.ExternalApiException("Access denied to leave API.", e);
            } else {
                throw new LeaveApiService.ExternalApiException("HTTP error fetching approved leave: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Failed to fetch approved leave for year: {}", year, e);
            throw new LeaveApiService.ExternalApiException("Failed to fetch approved leave from external API", e);
        }
    }

    @Override
    public LeaveApiResponse getApprovedLeaveForEmployee(Long employeeId, Integer year) throws LeaveApiService.ExternalApiException {
        try {
            String url = leaveApiBaseUrl + "/api/leave-requests/approved/" + employeeId + "/" + year;
            log.info("Fetching approved leave for employee {} from: {}", employeeId, url);
            
            // Token is added by AvailabilityEngineConfig interceptor
            HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

            ResponseEntity<LeaveApiResponse> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                LeaveApiResponse.class
            );
            
            LeaveApiResponse responseBody = response.getBody();
            
            if (responseBody == null || !responseBody.isSuccess()) {
                log.warn("Leave API returned unsuccessful response for employee: {} year: {}", employeeId, year);
                return new LeaveApiResponse();
            }
            
            log.info("Successfully fetched leave data for employee: {} year: {} with {} leave dates", 
                    employeeId, year, 
                    responseBody.getData() != null && responseBody.getData().getApprovedLeaveDates() != null ? 
                            responseBody.getData().getApprovedLeaveDates().size() : 0);
            return responseBody;
            
        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching approved leave for employee: {} year: {} - Status: {}, Message: {}", employeeId, year, e.getStatusCode(), e.getMessage());
            
            if (e.getStatusCode().value() == 401) {
                tokenService.invalidateToken();
                throw new LeaveApiService.ExternalApiException("Authentication failed with leave API. Token has been invalidated.", e);
            } else if (e.getStatusCode().value() == 403) {
                throw new LeaveApiService.ExternalApiException("Access denied to leave API.", e);
            } else {
                throw new LeaveApiService.ExternalApiException("HTTP error fetching approved leave: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Failed to fetch approved leave for employee: {} year: {}", employeeId, year, e);
            throw new LeaveApiService.ExternalApiException("Failed to fetch approved leave from external API", e);
        }
    }

    @Override
    public boolean isApiHealthy() {
        // No health check endpoint available - assume healthy if token service is working
        try {
            String token = tokenService.getAccessToken();
            return token != null && !token.isEmpty();
        } catch (Exception e) {
            log.warn("Leave API health check failed - token service error", e);
            return false;
        }
    }
}
