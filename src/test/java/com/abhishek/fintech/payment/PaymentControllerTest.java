package com.abhishek.fintech.payment;

import com.abhishek.fintech.common.exception.GlobalExceptionHandler;
import com.abhishek.fintech.payment.controller.PaymentController;
import com.abhishek.fintech.payment.dto.TransferRequest;
import com.abhishek.fintech.payment.dto.TransferResponse;
import com.abhishek.fintech.payment.entity.PaymentStatus;
import com.abhishek.fintech.payment.exception.InsufficientFundsException;
import com.abhishek.fintech.payment.service.PaymentService;
import com.abhishek.fintech.security.JwtAuthenticationFilter;
import com.abhishek.fintech.security.JwtService;
import com.abhishek.fintech.security.SecurityUser;
import com.abhishek.fintech.user.entity.Role;
import com.abhishek.fintech.user.entity.User;
import com.abhishek.fintech.user.entity.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User sampleUser;
    private UUID fromWalletId;
    private UUID toWalletId;

    @BeforeEach
    void setUp() {
        fromWalletId = UUID.randomUUID();
        toWalletId = UUID.randomUUID();

        sampleUser = User.builder()
                .email("alice@fintech.com")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        sampleUser.setId(UUID.randomUUID());

        SecurityUser securityUser = new SecurityUser(sampleUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                securityUser, null, securityUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldExecuteTransferSuccessfully() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .fromWalletId(fromWalletId)
                .toWalletId(toWalletId)
                .amount(BigDecimal.valueOf(250))
                .currency("INR")
                .description("Lunch bill")
                .build();

        TransferResponse response = TransferResponse.builder()
                .paymentId(UUID.randomUUID())
                .referenceId("PAY-123456")
                .fromWalletId(fromWalletId)
                .toWalletId(toWalletId)
                .amount(BigDecimal.valueOf(250).setScale(4, RoundingMode.HALF_UP))
                .currency("INR")
                .status(PaymentStatus.COMPLETED)
                .ledgerTransactionId(UUID.randomUUID())
                .description("Lunch bill")
                .timestamp(Instant.now())
                .build();

        when(paymentService.transfer(any(UUID.class), any(TransferRequest.class), eq("idemp-key-1")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .header("Idempotency-Key", "idemp-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceId").value("PAY-123456"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(250.0000));
    }

    @Test
    void shouldReturn422WhenInsufficientFunds() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .fromWalletId(fromWalletId)
                .toWalletId(toWalletId)
                .amount(BigDecimal.valueOf(5000))
                .currency("INR")
                .build();

        when(paymentService.transfer(any(UUID.class), any(TransferRequest.class), any()))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Insufficient Funds"));
    }
}
