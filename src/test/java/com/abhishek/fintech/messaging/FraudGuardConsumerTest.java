package com.abhishek.fintech.messaging;

import com.abhishek.fintech.messaging.consumer.FraudGuardConsumer;
import com.abhishek.fintech.messaging.event.PaymentCompletedEvent;
import com.abhishek.fintech.outbox.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudGuardConsumerTest {

    @Mock
    private OutboxService outboxService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @InjectMocks
    private FraudGuardConsumer fraudGuardConsumer;

    @Test
    void shouldEmitFraudAlertWhenTransactionExceedsHighValueThreshold() throws Exception {
        PaymentCompletedEvent highValueEvent = PaymentCompletedEvent.builder()
                .eventId("EVT-123")
                .paymentId(UUID.randomUUID())
                .referenceId("PAY-999")
                .senderWalletId(UUID.randomUUID())
                .receiverWalletId(UUID.randomUUID())
                .amount(new BigDecimal("150000.0000"))
                .currency("INR")
                .timestamp(Instant.now())
                .build();

        String payload = objectMapper.writeValueAsString(highValueEvent);

        fraudGuardConsumer.evaluatePaymentForFraud(payload);

        verify(outboxService).saveEvent(eq("FRAUD"), any(), eq("FRAUD_ALERT"), any());
    }

    @Test
    void shouldNotEmitFraudAlertForNormalTransactions() throws Exception {
        PaymentCompletedEvent normalEvent = PaymentCompletedEvent.builder()
                .eventId("EVT-456")
                .paymentId(UUID.randomUUID())
                .referenceId("PAY-111")
                .senderWalletId(UUID.randomUUID())
                .receiverWalletId(UUID.randomUUID())
                .amount(new BigDecimal("500.0000"))
                .currency("INR")
                .timestamp(Instant.now())
                .build();

        String payload = objectMapper.writeValueAsString(normalEvent);

        fraudGuardConsumer.evaluatePaymentForFraud(payload);

        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }
}
