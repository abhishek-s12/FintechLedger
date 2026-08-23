package com.abhishek.fintech;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = FintechLedgerApplication.class)
@ActiveProfiles("test")
class FintechLedgerApplicationTests {

    @Test
    void contextLoads() {
        assertTrue(true, "Application context configuration loads successfully");
    }
}
