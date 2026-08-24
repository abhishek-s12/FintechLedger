package com.abhishek.fintech.ratelimit;

import com.abhishek.fintech.config.RateLimitConfig;
import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private RateLimitConfig rateLimitConfig;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();
        rateLimitConfig.setEnabled(true);
        rateLimitConfig.setPaymentCapacity(3);
        rateLimitConfig.setPaymentRefillTokens(3);
        rateLimitConfig.setPaymentRefillPeriodSeconds(60);

        rateLimitConfig.setDefaultCapacity(5);
        rateLimitConfig.setDefaultRefillTokens(5);
        rateLimitConfig.setDefaultRefillPeriodSeconds(60);

        rateLimitService = new RateLimitService(rateLimitConfig);
    }

    @Test
    void shouldConsumeTokensUntilLimitExceeded() {
        String key = "test-client:127.0.0.1";
        String uri = "/api/v1/payments/transfer";

        // 3 tokens available
        ConsumptionProbe p1 = rateLimitService.tryConsume(key, uri);
        assertTrue(p1.isConsumed());
        assertEquals(2, p1.getRemainingTokens());

        ConsumptionProbe p2 = rateLimitService.tryConsume(key, uri);
        assertTrue(p2.isConsumed());
        assertEquals(1, p2.getRemainingTokens());

        ConsumptionProbe p3 = rateLimitService.tryConsume(key, uri);
        assertTrue(p3.isConsumed());
        assertEquals(0, p3.getRemainingTokens());

        // 4th request must be rejected
        ConsumptionProbe p4 = rateLimitService.tryConsume(key, uri);
        assertFalse(p4.isConsumed());
        assertTrue(p4.getNanosToWaitForRefill() > 0);
    }

    @Test
    void shouldReturnNullWhenRateLimitingDisabled() {
        rateLimitConfig.setEnabled(false);

        ConsumptionProbe probe = rateLimitService.tryConsume("key-1", "/api/v1/test");
        assertNull(probe);
    }
}
