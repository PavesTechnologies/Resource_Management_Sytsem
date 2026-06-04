package com.service_imple.ledger_service_impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service_interface.ledger_service_interface.HolidayService;
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
public class HolidayServiceImpl implements HolidayService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${external.api.leave.base-url}")
    private String holidayApiBaseUrl;
    
    @Value("${external.api.holiday.year-endpoint}")
    private String holidayYearEndpoint;

    private volatile boolean apiHealthy = true;

    @Override
    @Cacheable(value = "holidays", key = "#year")
    public Set<LocalDate> getHolidaysForYear(int year) throws HolidayApiException {
        return getHolidaysInternal(year);
    }

    private Set<LocalDate> getHolidaysInternal(int year) throws HolidayApiException {
        try {
            String url = holidayApiBaseUrl + holidayYearEndpoint.replace("{year}", String.valueOf(year));
            String responseBody = restTemplate.getForObject(url, String.class);
            Set<LocalDate> holidays = parseHolidayDates(responseBody);

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

    private Set<LocalDate> parseHolidayDates(String responseBody) throws Exception {
        Set<LocalDate> holidays = new HashSet<>();
        if (responseBody == null || responseBody.isBlank()) {
            return holidays;
        }

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode holidaysNode = resolveHolidayArray(root);
        if (holidaysNode == null || holidaysNode.isNull() || !holidaysNode.isArray()) {
            return holidays;
        }

        Iterator<JsonNode> iterator = holidaysNode.elements();
        while (iterator.hasNext()) {
            JsonNode holiday = iterator.next();
            String date = textValue(holiday, "holidayDate", "date");
            boolean active = true;
            if (holiday.has("isActive")) {
                active = holiday.path("isActive").asBoolean();
            } else if (holiday.has("active")) {
                active = holiday.path("active").asBoolean();
            }

            if (date != null && active) {
                holidays.add(LocalDate.parse(date));
            }
        }

        return holidays;
    }

    private JsonNode resolveHolidayArray(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (root.has("holidays")) {
            return root.get("holidays");
        }
        if (root.has("data")) {
            return root.get("data");
        }
        return null;
    }

    private String textValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.get(fieldName);
            if (valueNode != null && !valueNode.isNull()) {
                String value = valueNode.asText();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isApiHealthy() {
        return apiHealthy;
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

}
