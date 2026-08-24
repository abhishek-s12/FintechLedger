package com.abhishek.fintech.messaging;

import com.abhishek.fintech.messaging.consumer.AuditEventConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class AuditEventConsumerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuditEventConsumer auditEventConsumer;

    @Test
    void shouldConsumeAuditEventWithoutThrowing() {
        String eventPayload = "{\"eventType\":\"PAYMENT_COMPLETED\",\"eventId\":\"EVT-1\",\"referenceId\":\"PAY-123\"}";

        assertDoesNotThrow(() -> auditEventConsumer.consumeAuditEvent(eventPayload));
    }
}
