package com.abhishek.fintech.outbox;

import com.abhishek.fintech.config.KafkaConfig;
import com.abhishek.fintech.outbox.entity.OutboxEvent;
import com.abhishek.fintech.outbox.entity.OutboxStatus;
import com.abhishek.fintech.outbox.repository.OutboxEventRepository;
import com.abhishek.fintech.outbox.service.OutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    private OutboxEvent pendingEvent;

    @BeforeEach
    void setUp() {
        pendingEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("PAYMENT")
                .aggregateId("pay-123")
                .eventType("PAYMENT_COMPLETED")
                .payload("{\"paymentId\":\"pay-123\",\"amount\":500}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void shouldPublishPendingEventsToKafkaAndMarkAsPublished() {
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any()))
                .thenReturn(List.of(pendingEvent));

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq(KafkaConfig.TOPIC_PAYMENT_COMPLETED), eq("pay-123"), any()))
                .thenReturn(future);

        outboxPublisher.publishPendingEvents();

        assertEquals(OutboxStatus.PUBLISHED, pendingEvent.getStatus());
        verify(kafkaTemplate).send(eq(KafkaConfig.TOPIC_PAYMENT_COMPLETED), eq("pay-123"), any());
        verify(outboxEventRepository).save(pendingEvent);
    }

    @Test
    void shouldIncrementRetryCountOnKafkaFailure() {
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any()))
                .thenReturn(List.of(pendingEvent));

        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka broker down"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(failedFuture);

        outboxPublisher.publishPendingEvents();

        assertEquals(1, pendingEvent.getRetryCount());
        verify(outboxEventRepository).save(pendingEvent);
    }
}
