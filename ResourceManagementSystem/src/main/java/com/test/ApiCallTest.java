package com.test;

import com.service_interface.external_api_interface.HolidayApiService;
import com.service_interface.external_api_interface.LeaveApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * Test class to verify external API calls are working
 * Run this to check if APIs are accessible
 */
@Component
public class ApiCallTest implements CommandLineRunner {

    @Autowired
    private HolidayApiService holidayApiService;
    
    @Autowired
    private LeaveApiService leaveApiService;

    @Override
    public void run(String... args) throws Exception {
        // Debug code commented out - API tests disabled
        /*
        System.out.println("=== Testing External API Calls ===");
        
        // Test Holiday API
        System.out.println("\n1. Testing Holiday API Health...");
        boolean holidayHealthy = holidayApiService.isApiHealthy();
        System.out.println("Holiday API Healthy: " + holidayHealthy);
        
        if (holidayHealthy) {
            try {
                var holidays = holidayApiService.getHolidaysForYear(2024);
                System.out.println("Holiday API Call SUCCESS - Found " + holidays.size() + " holidays for 2024");
            } catch (Exception e) {
                System.out.println("Holiday API Call FAILED: " + e.getMessage());
            }
        }
        
        // Test Leave API
        System.out.println("\n2. Testing Leave API Health...");
        boolean leaveHealthy = leaveApiService.isApiHealthy();
        System.out.println("Leave API Healthy: " + leaveHealthy);
        
        if (leaveHealthy) {
            try {
                var leaveResponse = leaveApiService.getApprovedLeaveForYear(2024);
                System.out.println("Leave API Call SUCCESS - Response received for 2024");
            } catch (Exception e) {
                System.out.println("Leave API Call FAILED: " + e.getMessage());
            }
        }
        
        System.out.println("\n=== API Test Complete ===");
        */
    }
}
