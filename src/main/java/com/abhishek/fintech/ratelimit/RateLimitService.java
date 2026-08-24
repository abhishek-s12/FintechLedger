package com.abhishek.fintech.ratelimit;

import com.abhishek.fintech.config.RateLimitConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RateLimitConfig rateLimitConfig;
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    public ConsumptionProbe tryConsume(String key, String uri) {
        if (!rateLimitConfig.isEnabled()) {
            // Disabled: return mock probe indicating success
            return null;
        }

        Bucket bucket = bucketCache.computeIfAbsent(key, k -> createBucketForUri(uri));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public Bucket createBucketForUri(String uri) {
        if (uri != null && uri.contains("/api/v1/payments/transfer")) {
            return createBucket(
                    rateLimitConfig.getPaymentCapacity(),
                    rateLimitConfig.getPaymentRefillTokens(),
                    rateLimitConfig.getPaymentRefillPeriodSeconds()
            );
        } else if (uri != null && uri.contains("/api/v1/auth")) {
            return createBucket(
                    rateLimitConfig.getAuthCapacity(),
                    rateLimitConfig.getAuthRefillTokens(),
                    rateLimitConfig.getAuthRefillPeriodSeconds()
            );
        } else {
            return createBucket(
                    rateLimitConfig.getDefaultCapacity(),
                    rateLimitConfig.getDefaultRefillTokens(),
                    rateLimitConfig.getDefaultRefillPeriodSeconds()
            );
        }
    }

    private Bucket createBucket(int capacity, int refillTokens, int periodSeconds) {
        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.greedy(refillTokens, Duration.ofSeconds(periodSeconds))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    public void clearCache() {
        bucketCache.clear();
    }
}
