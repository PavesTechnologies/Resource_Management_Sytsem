package com.service_interface.ledger_service_interface;

import java.util.Set;
import java.time.LocalDate;

public interface LeaveService {
    
    
    Set<LocalDate> getApprovedLeaveForEmployee(Long employeeId, int year) throws LeaveApiException;
    
    boolean isApiHealthy();
    
    class LeaveApiException extends Exception {
        public LeaveApiException(String message) {
            super(message);
        }
        
        public LeaveApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
