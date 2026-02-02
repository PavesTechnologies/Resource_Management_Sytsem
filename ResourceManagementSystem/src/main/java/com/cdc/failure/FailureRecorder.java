package com.cdc.failure;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FailureRecorder {

    private static final Logger log =
            LoggerFactory.getLogger(FailureRecorder.class);

    private final CdcFailureRepository repository;

    /**
     * Record CDC failure safely.
     * This method MUST NEVER throw an exception.
     */
    public void recordFailure(
            String entityType,
            String entityId,
            String operation,
            String errorType,
            String errorMessage,
            String payload
    ) {
        try {
            CdcFailure failure = new CdcFailure();
            failure.setEntityType(entityType);
            failure.setEntityId(entityId);
            failure.setOperation(operation);
            failure.setErrorType(errorType);
            failure.setErrorMessage(errorMessage);
            failure.setPayload(payload);
            failure.setStatus("NEW");
            failure.setRetryCount(0);
            failure.setNextRetryAt(
                    LocalDateTime.now().plusMinutes(5)
            );

            repository.save(failure);

        } catch (Exception e) {
            // LAST LINE OF DEFENSE
            //log.error("❌ Failed to record CDC failure (ignored)", e);
        }
    }
}
