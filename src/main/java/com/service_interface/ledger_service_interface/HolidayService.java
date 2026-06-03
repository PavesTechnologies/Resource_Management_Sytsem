package com.service_interface.ledger_service_interface;

import java.util.Set;
import java.time.LocalDate;

public interface HolidayService {
    
    
    Set<LocalDate> getHolidaysForYear(int year) throws HolidayApiException;
    
    boolean isApiHealthy();
    
    class HolidayApiException extends Exception {
        public HolidayApiException(String message) {
            super(message);
        }
        
        public HolidayApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
