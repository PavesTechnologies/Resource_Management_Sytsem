package com.service_interface.ledger_service_interface;

import java.util.Set;
import java.time.LocalDate;

public interface HolidayService {
    
    // Cached wrapper method (internal use)
    Set<LocalDate> getHolidaysCached(int year) throws HolidayApiException;
    
    // Safe public method (used by ledger calculation)
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
