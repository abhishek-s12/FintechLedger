package com.abhishek.fintech.outbox.service;

import com.abhishek.fintech.outbox.entity.OutboxEvent;
import com.abhishek.fintech.outbox.entity.OutboxStatus;
import com.abhishek.fintech.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OutboxEvent saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String jsonPayload = payload instanceof String ? (String) payload : objectMapper.writeValueAsString(payload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(jsonPayload)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();

            OutboxEvent saved = outboxEventRepository.save(outboxEvent);
            log.info("Saved outbox event [{} - {}] for aggregate [{}:{}]",
                    saved.getId(), eventType, aggregateType, aggregateId);

            return saved;
        } catch (Exception e) {
            log.error("Failed to serialize and save outbox event for aggregate [{}:{}]", aggregateType, aggregateId, e);
            throw new IllegalStateException("Failed to persist transactional outbox event", e);
        }
    }
}
