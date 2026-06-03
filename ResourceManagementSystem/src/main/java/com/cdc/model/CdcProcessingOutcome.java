package com.cdc.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CdcProcessingOutcome {

    OutcomeType outcomeType;
    String message;
    String reasonCode;
    String lifecycleStatus;

    public static CdcProcessingOutcome success() {
        return CdcProcessingOutcome.builder()
                .outcomeType(OutcomeType.SUCCESS)
                .build();
    }

    public static CdcProcessingOutcome waiting(String message, String reasonCode, String lifecycleStatus) {
        return CdcProcessingOutcome.builder()
                .outcomeType(OutcomeType.WAITING_FOR_DEPENDENCY)
                .message(message)
                .reasonCode(reasonCode)
                .lifecycleStatus(lifecycleStatus)
                .build();
    }

    public static CdcProcessingOutcome cancelled(String message, String reasonCode, String lifecycleStatus) {
        return CdcProcessingOutcome.builder()
                .outcomeType(OutcomeType.CANCELLED)
                .message(message)
                .reasonCode(reasonCode)
                .lifecycleStatus(lifecycleStatus)
                .build();
    }

    public enum OutcomeType {
        SUCCESS,
        WAITING_FOR_DEPENDENCY,
        CANCELLED
    }
}
