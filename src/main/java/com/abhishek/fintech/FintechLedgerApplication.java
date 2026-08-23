package com.abhishek.fintech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FintechLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FintechLedgerApplication.class, args);
    }
}
