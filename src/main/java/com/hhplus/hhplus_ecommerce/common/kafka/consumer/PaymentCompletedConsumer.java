package com.hhplus.hhplus_ecommerce.common.kafka.consumer;

import com.hhplus.hhplus_ecommerce.common.kafka.KafkaTopics;
import com.hhplus.hhplus_ecommerce.common.kafka.message.PaymentCompletedMessage;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.external.DataPlatformClient;
import com.hhplus.hhplus_ecommerce.external.dto.OrderDataDto;
import com.hhplus.hhplus_ecommerce.order.domain.OrderItem;
import com.hhplus.hhplus_ecommerce.order.repository.OrderItemRepository;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedConsumer {

    private final DataPlatformClient dataPlatformClient;
    private final CouponRepository couponRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    /**
     * 결제 완료 메시지를 소비하여 데이터 플랫폼으로 전송
     * - 토픽: payment-completed
     * - 컨슈머 그룹: data-platform-service
     * - 수동 커밋: 처리 완료 후 명시적으로 커밋
     * - 멱등성: 동일한 orderId에 대한 중복 처리 방지 필요 (TODO)
     */
    @KafkaListener(
            topics = KafkaTopics.PAYMENT_COMPLETED,
            groupId = "data-platform-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentCompleted(
            @Payload PaymentCompletedMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("[Kafka Consumer] 결제 완료 메시지 수신 - Partition: {}, Offset: {}, OrderId: {}, UserId: {}",
                    partition, offset, message.getOrderId(), message.getUserId());

            // 쿠폰 정보 조회
            OrderDataDto.CouponData couponData = null;
            if (message.getCouponId() != null) {
                Coupon coupon = couponRepository.findById(message.getCouponId()).orElse(null);
                if (coupon != null) {
                    couponData = new OrderDataDto.CouponData(
                            coupon.getId(),
                            coupon.getName()
                    );
                }
            }

            // orderId로 주문 아이템 조회 및 변환
            var orderItems = orderItemRepository.findByOrderId(message.getOrderId());
            var items = orderItems.stream()
                    .map(item -> {
                        Product product = productRepository.findById(item.getProductId()).orElse(null);
                        String productName = product != null ? product.getName() : "Unknown";
                        return new OrderDataDto.OrderItemData(
                                item.getProductId(),
                                productName,
                                item.getQuantity(),
                                item.getUnitPrice()
                        );
                    })
                    .collect(Collectors.toList());

            // 데이터 플랫폼 전송 DTO 생성
            OrderDataDto orderData = new OrderDataDto(
                    message.getOrderId(),
                    message.getUserId(),
                    message.getTotalAmount(),
                    message.getDiscountAmount(),
                    message.getFinalAmount(),
                    couponData,
                    "COMPLETED",
                    items,
                    message.getCompletedAt()
            );

            // 데이터 플랫폼 전송
            dataPlatformClient.sendOrderData(orderData);

            // 처리 완료 후 명시적 커밋
            acknowledgment.acknowledge();

            log.info("[Kafka Consumer] 결제 완료 메시지 처리 완료 - Partition: {}, Offset: {}, OrderId: {}",
                    partition, offset, message.getOrderId());

        } catch (Exception e) {
            // 처리 실패 시 커밋하지 않음 → 재처리됨
            log.error("[Kafka Consumer] 결제 완료 메시지 처리 실패 - Partition: {}, Offset: {}, OrderId: {}, Error: {}",
                    partition, offset, message.getOrderId(), e.getMessage(), e);
            // TODO: DLT(Dead Letter Topic)로 전송하거나 재시도 로직 추가
        }
    }
}