package com.service_imple.external_api_impl;

import com.dto.external_dto.LeaveApiResponse;
import com.service_interface.external_api_interface.LeaveApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
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
                return new LeaveApiResponse();
            }
            
            return responseBody;
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                tokenService.invalidateToken();
                throw new LeaveApiService.ExternalApiException("Authentication failed with leave API. Token has been invalidated.", e);
            } else if (e.getStatusCode().value() == 403) {
                throw new LeaveApiService.ExternalApiException("Access denied to leave API.", e);
            } else {
                throw new LeaveApiService.ExternalApiException("HTTP error fetching approved leave: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new LeaveApiService.ExternalApiException("Failed to fetch approved leave from external API", e);
        }
    }

    @Override
    public LeaveApiResponse getApprovedLeaveForEmployee(Long employeeId, Integer year) throws LeaveApiService.ExternalApiException {
        try {
            String url = leaveApiBaseUrl + "/api/leave-requests/approved/" + employeeId + "/" + year;
            
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
                return new LeaveApiResponse();
            }
            
            return responseBody;
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                tokenService.invalidateToken();
                throw new LeaveApiService.ExternalApiException("Authentication failed with leave API. Token has been invalidated.", e);
            } else if (e.getStatusCode().value() == 403) {
                throw new LeaveApiService.ExternalApiException("Access denied to leave API.", e);
            } else {
                throw new LeaveApiService.ExternalApiException("HTTP error fetching approved leave: " + e.getMessage(), e);
            }
        } catch (Exception e) {
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
            return false;
        }
    }
}
