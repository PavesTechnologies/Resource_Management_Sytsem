package com.cdc.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "cdc.offer")
public class OfferLifecycleProperties {

    private final WaitingProperties waiting = new WaitingProperties();

    private final CompletedProperties completed = new CompletedProperties();

    private final Set<String> longWaitingStatuses = Set.of(
            "Accepted",
            "Submitted",
            "Verified",
            "Joining",
            "Joining Pending",
            "Rescheduled"
    );

    private final Set<String> immediateCancelStatuses = Set.of(
            "Rejected",
            "Created"
    );

    public boolean isWaitingStatus(String status) {
        return containsIgnoreCase(longWaitingStatuses, status);
    }

    public boolean isCompletedStatus(String status) {
        return "Completed".equalsIgnoreCase(normalize(status));
    }

    public boolean isImmediateCancelStatus(String status) {
        return containsIgnoreCase(immediateCancelStatuses, status);
    }

    public boolean isNonActionableStatus(String status) {
        return isImmediateCancelStatus(status);
    }

    public Duration waitingWindow() {
        return Duration.ofDays(waiting.getMaxDays());
    }

    public Duration waitingRetryInterval() {
        return Duration.ofHours(waiting.getRetryHours());
    }

    public Duration completedReconciliationWindow() {
        return Duration.ofMinutes(completed.getReconciliation().getMaxMinutes());
    }

    private boolean containsIgnoreCase(Set<String> values, String candidate) {
        String normalized = normalize(candidate);
        return normalized != null && values.stream().anyMatch(value -> value.equalsIgnoreCase(normalized));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    @Getter
    @Setter
    public static class WaitingProperties {

        @Min(1)
        private int maxDays = 45;

        @Min(1)
        private int retryHours = 6;

        @Min(1)
        private int releaseBatchSize = 25;
    }

    @Getter
    @Setter
    public static class CompletedProperties {

        private final ReconciliationProperties reconciliation = new ReconciliationProperties();
    }

    @Getter
    @Setter
    public static class ReconciliationProperties {

        @Min(1)
        private int maxMinutes = 60;
    }
}
