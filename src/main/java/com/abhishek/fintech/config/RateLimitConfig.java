package com.abhishek.fintech.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fintech.ratelimit")
@Data
public class RateLimitConfig {

    private boolean enabled = true;

    // Default general limit: 100 requests per minute
    private int defaultCapacity = 100;
    private int defaultRefillTokens = 100;
    private int defaultRefillPeriodSeconds = 60;

    // Payment transfer limit: 30 requests per minute
    private int paymentCapacity = 30;
    private int paymentRefillTokens = 30;
    private int paymentRefillPeriodSeconds = 60;

    // Authentication limit: 15 requests per minute
    private int authCapacity = 15;
    private int authRefillTokens = 15;
    private int authRefillPeriodSeconds = 60;
}
