package com.abhishek.fintech.outbox;

import com.abhishek.fintech.messaging.event.WalletCreatedEvent;
import com.abhishek.fintech.outbox.entity.OutboxEvent;
import com.abhishek.fintech.outbox.entity.OutboxStatus;
import com.abhishek.fintech.outbox.repository.OutboxEventRepository;
import com.abhishek.fintech.outbox.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @InjectMocks
    private OutboxService outboxService;

    @Test
    void shouldSaveOutboxEventSuccessfully() {
        WalletCreatedEvent event = WalletCreatedEvent.builder()
                .eventId("EVT-1")
                .eventType("WALLET_CREATED")
                .walletId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .currency("INR")
                .timestamp(Instant.now())
                .build();

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> {
            OutboxEvent oe = inv.getArgument(0);
            oe.setId(UUID.randomUUID());
            oe.setCreatedAt(Instant.now());
            return oe;
        });

        OutboxEvent saved = outboxService.saveEvent("WALLET", "123", "WALLET_CREATED", event);

        assertNotNull(saved);
        assertEquals("WALLET", saved.getAggregateType());
        assertEquals("123", saved.getAggregateId());
        assertEquals("WALLET_CREATED", saved.getEventType());
        assertEquals(OutboxStatus.PENDING, saved.getStatus());
        assertEquals(0, saved.getRetryCount());
        assertTrue(saved.getPayload().contains("WALLET_CREATED"));
        assertTrue(saved.getPayload().contains("INR"));

        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }
}
