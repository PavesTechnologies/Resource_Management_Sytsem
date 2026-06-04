package com.cdc.service;

import com.cdc.listener.EosCdcHandler;
import com.cdc.model.CdcProcessingOutcome;
import com.cdc.listener.PmsCdcHandler;
import com.cdc.payload.CdcEventPayload;
import com.cdc.payload.CdcPayloadCodec;
import com.entity.ledger_entities.LedgerEventLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InboxEventProcessor {

    private final CdcPayloadCodec cdcPayloadCodec;
    private final PmsCdcHandler pmsCdcHandler;
    private final EosCdcHandler eosCdcHandler;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CdcProcessingOutcome processSingleEvent(LedgerEventLog event) {
        CdcEventPayload payload = cdcPayloadCodec.deserialize(event.getPayload());

        if ("PMS".equalsIgnoreCase(event.getConnectorName())) {
            pmsCdcHandler.processInboxEvent(payload);
            return CdcProcessingOutcome.success();
        }

        if ("EOS".equalsIgnoreCase(event.getConnectorName())) {
            return eosCdcHandler.processInboxEvent(payload, event);
        }

        throw new IllegalStateException("Unsupported CDC connector: " + event.getConnectorName());
    }
}
