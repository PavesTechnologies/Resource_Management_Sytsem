package com.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminKPIDTO {
    private int totalClients;
    private int activeClients;
    private int activeProjects;
    private int totalRevenue;
}
