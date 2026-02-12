package com.service_interface.external_api_interface;

import com.dto.external_dto.HolidayDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface HolidayApiService {
    
    List<HolidayDto> getHolidaysForYear(Integer year) throws ExternalApiException;
    
    boolean isApiHealthy();
    
    class ExternalApiException extends Exception {
        public ExternalApiException(String message) {
            super(message);
        }
        
        public ExternalApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
