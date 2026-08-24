package com.abhishek.fintech.messaging.consumer;

import com.abhishek.fintech.config.KafkaConfig;
import com.abhishek.fintech.messaging.event.FraudAlertEvent;
import com.abhishek.fintech.messaging.event.PaymentCompletedEvent;
import com.abhishek.fintech.outbox.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudGuardConsumer {

    public static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("100000.0000");
    public static final BigDecimal CRITICAL_VALUE_THRESHOLD = new BigDecimal("500000.0000");

    private final ObjectMapper objectMapper;
    private final OutboxService outboxService;

    @KafkaListener(topics = KafkaConfig.TOPIC_PAYMENT_COMPLETED, groupId = "fintech-fraud-group")
    public void evaluatePaymentForFraud(String payload) {
        try {
            PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);
            BigDecimal amount = event.getAmount();

            if (amount != null && amount.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
                String severity = amount.compareTo(CRITICAL_VALUE_THRESHOLD) >= 0 ? "CRITICAL" : "HIGH";
                String reason = String.format("High value transaction detected: %s %s exceeds threshold %s",
                        amount, event.getCurrency(), HIGH_VALUE_THRESHOLD);

                FraudAlertEvent alert = FraudAlertEvent.builder()
                        .alertId("FRAUD-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase())
                        .ruleTriggered("HIGH_VALUE_TRANSACTION_SPIKE")
                        .severity(severity)
                        .paymentId(event.getPaymentId())
                        .paymentReferenceId(event.getReferenceId())
                        .senderWalletId(event.getSenderWalletId())
                        .receiverWalletId(event.getReceiverWalletId())
                        .amount(amount)
                        .currency(event.getCurrency())
                        .reason(reason)
                        .detectedAt(Instant.now())
                        .build();

                log.warn("[FRAUD GUARD ALERT] [Severity: {}] Payment [{}]: {}",
                        severity, event.getReferenceId(), reason);

                outboxService.saveEvent("FRAUD", alert.getAlertId(), "FRAUD_ALERT", alert);
            }
        } catch (Exception e) {
            log.error("Error analyzing payment event for fraud", e);
        }
    }
}
