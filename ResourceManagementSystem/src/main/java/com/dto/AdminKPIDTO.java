package com.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminKPIDTO {
    private int totalClients;
    private int activeClients;
    private int activeProjects;

    // The growth percentage (e.g., 15.5)
    private double growthPercentage;

    // Total clients from the previous year/period to show the "vs last year" value
    private int previousPeriodClientCount;

    // A flag to easily set UI colors (Green for true, Red for false)
    private boolean isGrowthPositive;

    // Optional: If you want to show a small chart in the UI,
    // this stores { "2024": 100, "2025": 120 }
    private Map<Integer, Integer> yearlyClientCounts;
}