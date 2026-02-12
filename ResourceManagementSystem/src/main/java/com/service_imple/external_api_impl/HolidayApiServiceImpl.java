package com.service_imple.external_api_impl;

import com.dto.external_dto.HolidayDto;
import com.service_interface.external_api_interface.HolidayApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class HolidayApiServiceImpl implements HolidayApiService {

    private final RestTemplate restTemplate;
    private final TokenService tokenService;
    
    @Value("${external.api.holiday.base-url:http://16.16.202.195:9999}")
    private String holidayApiBaseUrl;

    public HolidayApiServiceImpl(RestTemplate restTemplate, TokenService tokenService) {
        this.restTemplate = restTemplate;
        this.tokenService = tokenService;
    }

    @Override
    public List<HolidayDto> getHolidaysForYear(Integer year) throws HolidayApiService.ExternalApiException {
        try {
            String url = holidayApiBaseUrl + "/api/holidays/year/" + year;
            
            // Token is added by AvailabilityEngineConfig interceptor
            HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

            ResponseEntity<HolidayDto[]> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                HolidayDto[].class
            );
            
            HolidayDto[] holidays = response.getBody();
            
            if (holidays == null) {
                return List.of();
            }
            
            List<HolidayDto> holidayList = Arrays.asList(holidays);
            return holidayList;
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                tokenService.invalidateToken();
                throw new HolidayApiService.ExternalApiException("Authentication failed with holiday API. Token has been invalidated.", e);
            } else if (e.getStatusCode().value() == 403) {
                throw new HolidayApiService.ExternalApiException("Access denied to holiday API.", e);
            } else {
                throw new HolidayApiService.ExternalApiException("HTTP error fetching holidays: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new HolidayApiService.ExternalApiException("Failed to fetch holidays from external API", e);
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
