package com.service_imple.external_api_impl;

import com.dto.external.HolidayDto;
import com.service.TokenService;
import com.service_interface.external_api_interface.HolidayApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
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
            log.info("Fetching holidays from: {}", url);
            
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
                log.warn("Holiday API returned null response for year: {}", year);
                return List.of();
            }
            
            List<HolidayDto> holidayList = Arrays.asList(holidays);
            log.info("Successfully fetched {} holidays for year: {}", holidayList.size(), year);
            return holidayList;
            
        } catch (Exception e) {
            log.error("Failed to fetch holidays for year: {}", year, e);
            throw new HolidayApiService.ExternalApiException("Failed to fetch holidays from external API", e);
        }
    }

    @Override
    public boolean isApiHealthy() {
        try {
            String url = holidayApiBaseUrl + "/api/health";
            
            // Token is added by AvailabilityEngineConfig interceptor
            HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return true;
        } catch (Exception e) {
            log.warn("Holiday API health check failed", e);
            return false;
        }
    }
}
