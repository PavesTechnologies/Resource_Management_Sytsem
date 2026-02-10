package com.service_interface.external_api_interface;

import com.dto.external.LeaveApiResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface LeaveApiService {
    
    LeaveApiResponse getApprovedLeaveForYear(Integer year) throws ExternalApiException;
    
    LeaveApiResponse getApprovedLeaveForEmployee(Long employeeId, Integer year) throws ExternalApiException;
    
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
