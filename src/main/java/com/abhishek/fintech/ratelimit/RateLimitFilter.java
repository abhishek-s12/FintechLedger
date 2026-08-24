package com.abhishek.fintech.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Skip rate limiting for static swagger / actuator endpoints
        if (uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs") || uri.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String authHeader = request.getHeader("Authorization");
        String rateLimitKey = authHeader != null && authHeader.startsWith("Bearer ")
                ? "auth:" + authHeader.hashCode() + ":" + uri
                : "ip:" + clientIp + ":" + uri;

        ConsumptionProbe probe = rateLimitService.tryConsume(rateLimitKey, uri);

        if (probe != null && !probe.isConsumed()) {
            long waitForRefillSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
            log.warn("Rate limit exceeded for key [{}], uri [{}]. Retry after {}s",
                    rateLimitKey, uri, waitForRefillSeconds);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(waitForRefillSeconds));
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefillSeconds));

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. You have exceeded your API rate limit. Please retry after " + waitForRefillSeconds + " seconds."
            );
            problemDetail.setTitle("Rate Limit Exceeded");
            problemDetail.setType(URI.create("https://api.fintech.com/errors/rate-limit-exceeded"));
            problemDetail.setInstance(URI.create(uri));
            problemDetail.setProperty("timestamp", Instant.now());
            problemDetail.setProperty("traceId", UUID.randomUUID().toString());
            problemDetail.setProperty("retryAfterSeconds", waitForRefillSeconds);

            response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
            return;
        }

        if (probe != null) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
