package com.util.report_util;

import com.dto.report_dto.BenchPoolReportDTO;
import com.entity_enums.centralised_enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class RiskEvaluationHelper {

    private static final BigDecimal COST_THRESHOLD = new BigDecimal("80000.00");
    private static final long HIGH_BENCH_DAYS_THRESHOLD = 30;
    private static final long MEDIUM_BENCH_DAYS_THRESHOLD = 15;
    private static final long HIGH_COST_BENCH_DAYS_THRESHOLD = 20;

    public void evaluateRisk(BenchPoolReportDTO report) {
        RiskLevel riskLevel = RiskLevel.LOW;
        String riskType = null;
        String recommendedAction = "Monitor resource allocation";

        if (report.getBenchDays() > HIGH_BENCH_DAYS_THRESHOLD) {
            riskLevel = RiskLevel.HIGH;
            riskType = "LONG_BENCH";
            recommendedAction = "Urgent: Allocate to project immediately";
        } else if (report.getBenchDays() > MEDIUM_BENCH_DAYS_THRESHOLD) {
            riskLevel = RiskLevel.MEDIUM;
            riskType = "LONG_BENCH";
            recommendedAction = "Priority: Consider for upcoming projects";
        }

        if (report.getCost() != null && report.getCost().compareTo(COST_THRESHOLD) > 0 && 
            report.getBenchDays() > HIGH_COST_BENCH_DAYS_THRESHOLD) {
            riskLevel = RiskLevel.HIGH;
            if (riskType == null) {
                riskType = "HIGH_COST";
            } else {
                riskType = "LONG_BENCH_AND_HIGH_COST";
            }
            recommendedAction = "Critical: High-cost resource on bench - expedite allocation";
        }

        report.setRiskLevel(riskLevel);
        report.setRiskType(riskType);
        report.setRecommendedAction(recommendedAction);
    }

    public long calculateBenchDays(LocalDate lastAllocationEndDate) {
        if (lastAllocationEndDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(lastAllocationEndDate, LocalDate.now());
    }
}
