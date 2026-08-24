package com.abhishek.fintech.outbox.service;

import com.abhishek.fintech.config.KafkaConfig;
import com.abhishek.fintech.outbox.entity.OutboxEvent;
import com.abhishek.fintech.outbox.entity.OutboxStatus;
import com.abhishek.fintech.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final int MAX_RETRIES = 5;
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${fintech.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE)
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events for publishing", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            String topic = resolveTopicForEvent(event.getEventType());
            if (topic == null) {
                log.error("Unknown event type [{}] for outbox event [{}]", event.getEventType(), event.getId());
                event.setStatus(OutboxStatus.FAILED);
                outboxEventRepository.save(event);
                continue;
            }

            try {
                // Publish using aggregateId as the message key to preserve partition ordering
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                event.setStatus(OutboxStatus.PUBLISHED);
                                event.setProcessedAt(Instant.now());
                                outboxEventRepository.save(event);
                                log.info("Successfully published outbox event [{}] to topic [{}]",
                                        event.getId(), topic);
                            } else {
                                log.error("Failed to publish outbox event [{}] to topic [{}]",
                                        event.getId(), topic, ex);
                                handlePublishFailure(event);
                            }
                        });
            } catch (Exception e) {
                log.error("Exception occurred while sending outbox event [{}] to Kafka", event.getId(), e);
                handlePublishFailure(event);
            }
        }
    }

    private void handlePublishFailure(OutboxEvent event) {
        event.setRetryCount(event.getRetryCount() + 1);
        if (event.getRetryCount() >= MAX_RETRIES) {
            event.setStatus(OutboxStatus.FAILED);
            log.error("Outbox event [{}] exceeded maximum retries ({}) and marked as FAILED",
                    event.getId(), MAX_RETRIES);
        }
        outboxEventRepository.save(event);
    }

    public String resolveTopicForEvent(String eventType) {
        if (eventType == null) return null;
        return switch (eventType.toUpperCase()) {
            case "PAYMENT_COMPLETED" -> KafkaConfig.TOPIC_PAYMENT_COMPLETED;
            case "PAYMENT_FAILED" -> KafkaConfig.TOPIC_PAYMENT_FAILED;
            case "WALLET_CREATED" -> KafkaConfig.TOPIC_WALLET_CREATED;
            case "FRAUD_ALERT" -> KafkaConfig.TOPIC_FRAUD_ALERT;
            default -> null;
        };
    }
}
