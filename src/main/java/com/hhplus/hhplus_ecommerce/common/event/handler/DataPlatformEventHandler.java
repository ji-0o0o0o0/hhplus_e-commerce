package com.hhplus.hhplus_ecommerce.common.event.handler;

import com.hhplus.hhplus_ecommerce.common.event.PaymentCompletedEvent;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.external.DataPlatformClient;
import com.hhplus.hhplus_ecommerce.external.dto.OrderDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformEventHandler {

    private final DataPlatformClient dataPlatformClient;
    private final CouponRepository couponRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            log.info("[이벤트] 결제 완료 이벤트 수신 - OrderId: {}", event.orderId());

            // 쿠폰 정보 조회
            OrderDataDto.CouponData couponData = null;
            if (event.couponId() != null) {
                Coupon coupon = couponRepository.findById(event.couponId()).orElse(null);
                if (coupon != null) {
                    couponData = new OrderDataDto.CouponData(
                            coupon.getId(),
                            coupon.getName()
                    );
                }
            }

            // 주문 아이템 변환
            var items = event.items().stream()
                    .map(item -> new OrderDataDto.OrderItemData(
                            item.productId(),
                            item.productName(),
                            item.quantity(),
                            item.unitPrice()
                    ))
                    .collect(Collectors.toList());

            // 데이터 플랫폼 전송 DTO 생성
            OrderDataDto orderData = new OrderDataDto(
                    event.orderId(),
                    event.userId(),
                    event.totalAmount(),
                    event.discountAmount(),
                    event.finalAmount(),
                    couponData,
                    "COMPLETED",
                    items,
                    event.completedAt()
            );

            // 데이터 플랫폼 전송
            dataPlatformClient.sendOrderData(orderData);

        } catch (Exception e) {
            // 데이터 플랫폼 전송 실패해도 결제는 완료됨 (부가 로직)
            log.warn("[이벤트] 데이터 플랫폼 전송 실패 - OrderId: {}, Error: {}",
                    event.orderId(), e.getMessage());
        }
    }
}