package com.abhishek.fintech.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/v1/payments/transfer");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void shouldPassWhenTokensAreAvailable() throws Exception {
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(5L);

        when(rateLimitService.tryConsume(anyString(), eq("/api/v1/payments/transfer"))).thenReturn(probe);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-Rate-Limit-Remaining", "5");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldReturn429WhenTokensAreExhausted() throws Exception {
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getNanosToWaitForRefill()).thenReturn(10_000_000_000L); // 10 seconds

        when(rateLimitService.tryConsume(anyString(), eq("/api/v1/payments/transfer"))).thenReturn(probe);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setHeader(eq("Retry-After"), eq("10"));
        verify(filterChain, never()).doFilter(request, response);
    }
}
