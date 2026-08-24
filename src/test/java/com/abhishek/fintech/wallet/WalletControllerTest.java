package com.abhishek.fintech.wallet;

import com.abhishek.fintech.common.exception.GlobalExceptionHandler;
import com.abhishek.fintech.security.JwtAuthenticationFilter;
import com.abhishek.fintech.security.JwtService;
import com.abhishek.fintech.security.SecurityUser;
import com.abhishek.fintech.user.entity.Role;
import com.abhishek.fintech.user.entity.User;
import com.abhishek.fintech.user.entity.UserStatus;
import com.abhishek.fintech.wallet.controller.WalletController;
import com.abhishek.fintech.wallet.dto.CreateWalletRequest;
import com.abhishek.fintech.wallet.dto.DepositRequest;
import com.abhishek.fintech.wallet.dto.WalletBalanceResponse;
import com.abhishek.fintech.wallet.dto.WalletResponse;
import com.abhishek.fintech.wallet.entity.WalletStatus;
import com.abhishek.fintech.wallet.service.WalletService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.abhishek.fintech.ratelimit.RateLimitFilter rateLimitFilter;

    private SecurityUser testSecurityUser;
    private UUID userId;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();

        User user = User.builder()
                .email("alice@fintech.com")
                .passwordHash("hashed")
                .firstName("Alice")
                .lastName("Smith")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(userId);

        testSecurityUser = new SecurityUser(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testSecurityUser, null, testSecurityUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldCreateWalletSuccessfully() throws Exception {
        CreateWalletRequest request = CreateWalletRequest.builder().currency("USD").build();

        WalletResponse response = WalletResponse.builder()
                .id(walletId)
                .userId(userId)
                .currency("USD")
                .balance(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .status(WalletStatus.ACTIVE)
                .version(0L)
                .createdAt(Instant.now())
                .build();

        when(walletService.createWallet(eq(userId), any(CreateWalletRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(walletId.toString()))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldListWallets() throws Exception {
        WalletResponse response = WalletResponse.builder()
                .id(walletId)
                .userId(userId)
                .currency("INR")
                .balance(BigDecimal.valueOf(100).setScale(4, RoundingMode.HALF_UP))
                .status(WalletStatus.ACTIVE)
                .build();

        when(walletService.getWalletsByUserId(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/wallets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currency").value("INR"))
                .andExpect(jsonPath("$[0].id").value(walletId.toString()));
    }

    @Test
    void shouldGetWalletBalance() throws Exception {
        WalletBalanceResponse balanceResponse = WalletBalanceResponse.builder()
                .walletId(walletId)
                .currency("INR")
                .balance(BigDecimal.valueOf(2500).setScale(4, RoundingMode.HALF_UP))
                .status(WalletStatus.ACTIVE)
                .build();

        when(walletService.getWalletBalance(walletId, userId)).thenReturn(balanceResponse);

        mockMvc.perform(get("/api/v1/wallets/" + walletId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(2500.0000));
    }

    @Test
    void shouldDepositFunds() throws Exception {
        DepositRequest depositRequest = DepositRequest.builder()
                .amount(BigDecimal.valueOf(500))
                .description("Funding")
                .build();

        WalletResponse updatedWallet = WalletResponse.builder()
                .id(walletId)
                .userId(userId)
                .currency("INR")
                .balance(BigDecimal.valueOf(500).setScale(4, RoundingMode.HALF_UP))
                .status(WalletStatus.ACTIVE)
                .build();

        when(walletService.deposit(eq(walletId), eq(userId), any(DepositRequest.class))).thenReturn(updatedWallet);

        mockMvc.perform(post("/api/v1/wallets/" + walletId + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.0000));
    }
}
