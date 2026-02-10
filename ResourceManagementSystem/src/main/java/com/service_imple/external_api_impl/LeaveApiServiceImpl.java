package com.service_imple.external_api_impl;

import com.dto.external.LeaveApiResponse;
import com.service_interface.external_api_interface.LeaveApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class LeaveApiServiceImpl implements LeaveApiService {

    private final RestTemplate restTemplate;

    @Value("${external.api.holiday.base-url:http://16.16.202.195:9999}")
    private String leaveApiBaseUrl;

    public LeaveApiServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public LeaveApiResponse getApprovedLeaveForYear(Integer year) throws LeaveApiService.ExternalApiException {
        try {
            String url = leaveApiBaseUrl + "/api/leave-requests/approved/" + year;
            log.info("Fetching approved leave from: {}", url);
            
            LeaveApiResponse response = restTemplate.getForObject(url, LeaveApiResponse.class);
            
            if (response == null || !response.isSuccess()) {
                log.warn("Leave API returned unsuccessful response for year: {}", year);
                return new LeaveApiResponse();
            }
            
            log.info("Successfully fetched leave data for year: {}", year);
            return response;
            
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
            
            LeaveApiResponse response = restTemplate.getForObject(url, LeaveApiResponse.class);
            
            if (response == null || !response.isSuccess()) {
                log.warn("Leave API returned unsuccessful response for employee: {} year: {}", employeeId, year);
                return new LeaveApiResponse();
            }
            
            log.info("Successfully fetched leave data for employee: {} year: {} with {} leave dates", 
                    employeeId, year, 
                    response.getData() != null && response.getData().getApprovedLeaveDates() != null ? 
                            response.getData().getApprovedLeaveDates().size() : 0);
            return response;
            
        } catch (Exception e) {
            log.error("Failed to fetch approved leave for employee: {} year: {}", employeeId, year, e);
            throw new LeaveApiService.ExternalApiException("Failed to fetch approved leave from external API", e);
        }
    }

    @Override
    public boolean isApiHealthy() {
        try {
            String url = leaveApiBaseUrl + "/api/health";
            restTemplate.getForObject(url, String.class);
            return true;
        } catch (Exception e) {
            log.warn("Leave API health check failed", e);
            return false;
        }
    }
}
