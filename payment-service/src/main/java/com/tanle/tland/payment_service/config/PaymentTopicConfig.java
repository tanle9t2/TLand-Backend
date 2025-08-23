package com.tanle.tland.payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class PaymentTopicConfig {
    @Bean
    public NewTopic orderTopic() {
        return TopicBuilder
                .name("tland-payment-topic")
                .replicas(2)
                .partitions(2)
                .build();
    }
}
