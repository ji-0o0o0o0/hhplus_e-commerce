package com.hhplus.hhplus_ecommerce.common.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;


@Configuration
public class KafkaConfig {

    /**
     * 결제 완료 토픽 생성
     * - 파티션: 6개 (병렬 처리)
     * - Replication Factor: 1 (로컬 환경)
     */
    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_COMPLETED)
                .partitions(6)
                .replicas(1)
                .build();
    }

    /**
     * 주문 완료 토픽 생성
     * - 파티션: 6개 (병렬 처리)
     * - Replication Factor: 1 (로컬 환경)
     */
    @Bean
    public NewTopic orderCompletedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_COMPLETED)
                .partitions(6)
                .replicas(1)
                .build();
    }
}
