package com.abhishek.fintech.messaging.consumer;

import com.abhishek.fintech.config.KafkaConfig;
import com.abhishek.fintech.messaging.event.PaymentCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.TOPIC_PAYMENT_COMPLETED, groupId = "fintech-notification-group")
    public void consumePaymentNotification(String payload) {
        try {
            PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);

            log.info("[NOTIFICATION DISPATCH] [SMS/EMAIL] Sent DEBIT alert to sender user [{}] for {} {} (Ref: {})",
                    event.getSenderUserId(), event.getAmount(), event.getCurrency(), event.getReferenceId());

            log.info("[NOTIFICATION DISPATCH] [SMS/EMAIL] Sent CREDIT alert to receiver user [{}] for {} {} (Ref: {})",
                    event.getReceiverUserId(), event.getAmount(), event.getCurrency(), event.getReferenceId());
        } catch (Exception e) {
            log.error("Failed to process payment notification dispatch", e);
        }
    }
}
