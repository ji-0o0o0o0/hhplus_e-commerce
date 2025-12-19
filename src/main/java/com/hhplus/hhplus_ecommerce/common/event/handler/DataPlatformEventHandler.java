package com.hhplus.hhplus_ecommerce.common.event.handler;

import com.hhplus.hhplus_ecommerce.common.event.PaymentCompletedEvent;
import com.hhplus.hhplus_ecommerce.common.kafka.message.PaymentCompletedMessage;
import com.hhplus.hhplus_ecommerce.common.kafka.producer.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformEventHandler {

    private final KafkaProducerService kafkaProducerService;

    /**
     * 결제 완료 이벤트를 Kafka로 발행
     * - @TransactionalEventListener(AFTER_COMMIT): 트랜잭션 커밋 후 실행
     * - Kafka에 메시지 발행 후 책임 종료
     * - 데이터 플랫폼, 알림 서비스 등은 각자 Kafka Consumer로 메시지 소비
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            log.info("[이벤트] 결제 완료 이벤트 수신 - OrderId: {}", event.orderId());

            // Kafka 메시지 생성 (items는 Consumer에서 orderId로 조회)
            PaymentCompletedMessage message = new PaymentCompletedMessage(
                    event.orderId(),
                    event.userId(),
                    event.totalAmount(),
                    event.discountAmount(),
                    event.finalAmount(),
                    event.couponId(),
                    event.completedAt()
            );

            // Kafka로 메시지 발행
            kafkaProducerService.sendPaymentCompletedMessage(message);

        } catch (Exception e) {
            // Kafka 발행 실패해도 결제는 완료됨 (부가 로직)
            log.warn("[이벤트] Kafka 메시지 발행 실패 - OrderId: {}, Error: {}",
                    event.orderId(), e.getMessage(), e);
        }
    }
}