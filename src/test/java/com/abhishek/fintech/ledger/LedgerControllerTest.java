package com.abhishek.fintech.ledger;

import com.abhishek.fintech.common.exception.GlobalExceptionHandler;
import com.abhishek.fintech.ledger.controller.LedgerController;
import com.abhishek.fintech.ledger.dto.LedgerEntryResponse;
import com.abhishek.fintech.ledger.dto.LedgerTransactionResponse;
import com.abhishek.fintech.ledger.entity.EntryType;
import com.abhishek.fintech.ledger.entity.TransactionStatus;
import com.abhishek.fintech.ledger.entity.TransactionType;
import com.abhishek.fintech.ledger.service.LedgerService;
import com.abhishek.fintech.security.JwtAuthenticationFilter;
import com.abhishek.fintech.security.JwtService;
import com.abhishek.fintech.security.SecurityUser;
import com.abhishek.fintech.user.entity.Role;
import com.abhishek.fintech.user.entity.User;
import com.abhishek.fintech.user.entity.UserStatus;
import com.abhishek.fintech.wallet.dto.WalletResponse;
import com.abhishek.fintech.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LedgerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LedgerService ledgerService;

    @MockBean
    private WalletService walletService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User sampleUser;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        walletId = UUID.randomUUID();

        sampleUser = User.builder()
                .email("alice@fintech.com")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        sampleUser.setId(userId);

        SecurityUser securityUser = new SecurityUser(sampleUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                securityUser, null, securityUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldGetLedgerTransactionByReference() throws Exception {
        LedgerTransactionResponse response = LedgerTransactionResponse.builder()
                .id(UUID.randomUUID())
                .referenceId("TX-123456")
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .description("Lunch bill")
                .createdAt(Instant.now())
                .entries(List.of())
                .build();

        when(ledgerService.getTransactionByReferenceId("TX-123456")).thenReturn(response);

        mockMvc.perform(get("/api/v1/ledger/transactions/TX-123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceId").value("TX-123456"))
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldGetWalletEntriesStatement() throws Exception {
        LedgerEntryResponse entry = LedgerEntryResponse.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .walletId(walletId)
                .entryType(EntryType.CREDIT)
                .amount(BigDecimal.valueOf(500).setScale(4, RoundingMode.HALF_UP))
                .currency("INR")
                .createdAt(Instant.now())
                .build();

        when(walletService.getWalletById(eq(walletId), eq(sampleUser.getId())))
                .thenReturn(WalletResponse.builder().id(walletId).build());

        when(ledgerService.getWalletLedgerEntries(eq(walletId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry)));

        mockMvc.perform(get("/api/v1/ledger/wallets/" + walletId + "/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entryType").value("CREDIT"))
                .andExpect(jsonPath("$.content[0].amount").value(500.0000));
    }
}
