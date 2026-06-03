package com.cdc.execution;

import com.cdc.failure.FailureRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CdcSafeExecutor {

    private final FailureRecorder failureRecorder;

    /**
     * Executes CDC logic safely.
     * Any exception is captured, stored, and rethrown so Debezium does not
     * acknowledge progress without durable recovery state.
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

            log.error("CDC execution failed for entityType={}, entityId={}, operation={}: {}",
                    entityType, entityId, operation, ex.getMessage(), ex);
            throw ex;
        }
    }
}
