package com.hhplus.hhplus_ecommerce.common.kafka.producer;

import com.hhplus.hhplus_ecommerce.common.kafka.KafkaTopics;
import com.hhplus.hhplus_ecommerce.common.kafka.message.CouponIssueRequestMessage;
import com.hhplus.hhplus_ecommerce.common.kafka.message.PaymentCompletedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 결제 완료 메시지 발행
     * - 토픽: payment-completed
     * - 키: userId (같은 사용자의 결제는 순서 보장)
     */
    public void sendPaymentCompletedMessage(PaymentCompletedMessage message) {
        String key = String.valueOf(message.getUserId());
        String topic = KafkaTopics.PAYMENT_COMPLETED;

        log.info("[Kafka Producer] 결제 완료 메시지 발행 시작 - Topic: {}, Key: {}, OrderId: {}",
                topic, key, message.getOrderId());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[Kafka Producer] 결제 완료 메시지 발행 성공 - Topic: {}, Partition: {}, Offset: {}, OrderId: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        message.getOrderId());
            } else {
                log.warn("[Kafka Producer] 결제 완료 메시지 발행 실패 - Topic: {}, OrderId: {}, Error: {}",
                        topic, message.getOrderId(), ex.getMessage(), ex);
            }
        });
    }

    /**
     * 쿠폰 발급 요청 메시지 발행
     * - 토픽: coupon-issue-request
     * - 키: couponId (같은 쿠폰의 발급 요청은 순서 보장)
     */
    public void sendCouponIssueRequest(CouponIssueRequestMessage message) {
        String key = String.valueOf(message.getCouponId());
        String topic = KafkaTopics.COUPON_ISSUE_REQUEST;

        log.info("[Kafka Producer] 쿠폰 발급 요청 메시지 발행 시작 - Topic: {}, Key: {}, RequestId: {}, UserId: {}, CouponId: {}",
                topic, key, message.getRequestId(), message.getUserId(), message.getCouponId());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[Kafka Producer] 쿠폰 발급 요청 메시지 발행 성공 - Topic: {}, Partition: {}, Offset: {}, RequestId: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        message.getRequestId());
            } else {
                log.warn("[Kafka Producer] 쿠폰 발급 요청 메시지 발행 실패 - Topic: {}, RequestId: {}, Error: {}",
                        topic, message.getRequestId(), ex.getMessage(), ex);
            }
        });
    }
}