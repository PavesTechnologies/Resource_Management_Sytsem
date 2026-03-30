package com.service_imple.ledger_service_impl;

import com.service_interface.ledger_service_interface.HolidayService;
import com.dto.common.ApiHealthResponse;
import com.dto.common.ExternalHolidayDto;
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
public class HolidayServiceImpl implements HolidayService {

    private final RestTemplate restTemplate;
    
    @Value("${external.api.holiday.base-url}")
    private String holidayApiBaseUrl;
    
    @Value("${external.api.holiday.year-endpoint}")
    private String holidayYearEndpoint;

    private volatile boolean apiHealthy = true;
    private volatile long lastHealthCheck = 0;
    private static final long HEALTH_CHECK_INTERVAL_MS = 60000;

    @Override
    @Cacheable(value = "holidays", key = "#year", unless = "#result == null || #result.isEmpty()")
    @Retryable(value = {ResourceAccessException.class, HttpClientErrorException.class}, 
               maxAttempts = 3, backoff = @org.springframework.retry.annotation.Backoff(delay = 1000, multiplier = 2))
    public Set<LocalDate> getHolidaysCached(int year) throws HolidayApiException {
        return getHolidaysInternal(year);
    }

    @Override
    public Set<LocalDate> getHolidaysForYear(int year) throws HolidayApiException {
        try {
            return getHolidaysCached(year);
        } catch (Exception ex) {
            log.warn("Cache failure, falling back to API for holidays year {}: {}", year, ex.getMessage());
            return getHolidaysInternal(year);
        }
    }

    private Set<LocalDate> getHolidaysInternal(int year) throws HolidayApiException {
        try {
            checkApiHealth();
            
            String url = holidayApiBaseUrl + holidayYearEndpoint.replace("{year}", String.valueOf(year));
            HolidayApiResponse response = restTemplate.getForObject(url, HolidayApiResponse.class);
            
            if (response == null || response.getHolidays() == null) {
                apiHealthy = false;
                return new HashSet<>();
            }

            Set<LocalDate> holidays = new HashSet<>();
            for (ExternalHolidayDto holiday : response.getHolidays()) {
                if (holiday.getDate() != null && holiday.isActive()) {
                    holidays.add(LocalDate.parse(holiday.getDate()));
                }
            }

            apiHealthy = true;
            return holidays;

        } catch (ResourceAccessException e) {
            apiHealthy = false;
            log.error("Holiday API connection failed for year {}: {}", year, e.getMessage());
            throw new HolidayApiException("Holiday API connection failed", e);
        } catch (HttpClientErrorException e) {
            apiHealthy = false;
            log.error("Holiday API returned error for year {}: {}", year, e.getMessage());
            throw new HolidayApiException("Holiday API error: " + e.getMessage(), e);
        } catch (Exception e) {
            apiHealthy = false;
            log.error("Unexpected error fetching holidays for year {}: {}", year, e.getMessage());
            throw new HolidayApiException("Unexpected error", e);
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

    private void checkApiHealth() throws HolidayApiException {
        // Health check not implemented - API assumed to be available
        // Add health check implementation if needed in the future
    }

    private void performHealthCheck() {
        // Health check not implemented - API assumed to be healthy
        apiHealthy = true;
    }

    @Retryable(value = {ResourceAccessException.class}, maxAttempts = 2, backoff = @org.springframework.retry.annotation.Backoff(delay = 500))
    public CompletableFuture<Set<LocalDate>> getHolidaysForYearAsync(int year) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getHolidaysForYear(year);
            } catch (HolidayApiException e) {
                return new HashSet<>();
            }
        });
    }

    public void preloadHolidaysForYears(int startYear, int endYear) {
        for (int year = startYear; year <= endYear; year++) {
            try {
                getHolidaysForYear(year);
            } catch (HolidayApiException e) {
                log.warn("Failed to preload holidays for year {}: {}", year, e.getMessage());
            }
        }
    }

    public static class HolidayApiResponse {
        private Set<ExternalHolidayDto> holidays;
        private int year;
        private String lastUpdated;

        public Set<ExternalHolidayDto> getHolidays() { return holidays; }
        public void setHolidays(Set<ExternalHolidayDto> holidays) { this.holidays = holidays; }
        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        public String getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
    }
}
