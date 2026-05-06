package com.cdc.execution;

import com.cdc.failure.FailureRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CdcSafeExecutor {

    private final FailureRecorder failureRecorder;

    /**
     * Executes CDC logic safely.
     * Any exception is captured and stored.
     */
    public void execute(
            String entityType,
            String entityId,
            String operation,
            String payload,
            Runnable action
    ) {
        try {
            action.run();
        } catch (Exception ex) {

            failureRecorder.recordFailure(
                    entityType,
                    entityId,
                    operation,
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    payload
            );

            // IMPORTANT:
            // Exception is swallowed to keep Debezium alive
        }
    }
}
