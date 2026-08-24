package com.abhishek.fintech.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableKafka
@EnableScheduling
public class KafkaConfig {

    public static final String TOPIC_PAYMENT_COMPLETED = "fintech.payment.completed";
    public static final String TOPIC_PAYMENT_FAILED = "fintech.payment.failed";
    public static final String TOPIC_WALLET_CREATED = "fintech.wallet.created";
    public static final String TOPIC_FRAUD_ALERT = "fintech.fraud.alert";

    @Bean
    public KafkaAdmin kafkaAdmin(KafkaProperties properties) {
        KafkaAdmin admin = new KafkaAdmin(properties.buildAdminProperties(null));
        admin.setFatalIfBrokerNotAvailable(false);
        admin.setAutoCreate(false);
        return admin;
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_COMPLETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_FAILED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic walletCreatedTopic() {
        return TopicBuilder.name(TOPIC_WALLET_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudAlertTopic() {
        return TopicBuilder.name(TOPIC_FRAUD_ALERT)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
