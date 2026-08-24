package com.abhishek.fintech.messaging.consumer;

import com.abhishek.fintech.config.KafkaConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {KafkaConfig.TOPIC_PAYMENT_COMPLETED, KafkaConfig.TOPIC_WALLET_CREATED, KafkaConfig.TOPIC_PAYMENT_FAILED},
            groupId = "fintech-audit-group"
    )
    public void consumeAuditEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "UNKNOWN";
            String eventId = node.has("eventId") ? node.get("eventId").asText() : "N/A";
            String refId = node.has("referenceId") ? node.get("referenceId").asText() : "N/A";

            log.info("[AUDIT-LOG] Event: {} | EventId: {} | Ref: {} | RawPayload: {}",
                    eventType, eventId, refId, payload);
        } catch (Exception e) {
            log.error("Failed to process audit event log", e);
        }
    }
}
