package com.cdc.listener;

import com.cdc.event.EmployeeDetailsCommittedEvent;
import com.cdc.service.EosResourceSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EosWaitingOfferReleaseListener {

    private final EosResourceSyncService eosResourceSyncService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmployeeDetailsCommitted(EmployeeDetailsCommittedEvent event) {
        try {
            eosResourceSyncService.releaseWaitingOfferEventsAfterCommit(
                    event.getEmployeeId(),
                    event.getWorkEmail()
            );
        } catch (Exception ex) {
            log.error("Failed to release waiting offer_letter_details events after employee_details commit for employeeId={}, workEmail={}: {}",
                    event.getEmployeeId(), event.getWorkEmail(), ex.getMessage(), ex);
        }
    }
}
