package com.hhplus.hhplus_ecommerce.common.event.handler;

import com.hhplus.hhplus_ecommerce.common.event.PaymentCompletedEvent;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRankingEventHandler {

    private final ProductRankingRepository productRankingRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            log.info("[이벤트] 인기 상품 집계 시작 - OrderId: {}", event.orderId());

            // 각 상품별 판매량 증가
            for (PaymentCompletedEvent.OrderItemInfo item : event.items()) {
                productRankingRepository.incrementSales(item.productId(), item.quantity());
                log.debug("[인기 상품] ProductId: {}, Quantity: {}", item.productId(), item.quantity());
            }

            log.info("[이벤트] 인기 상품 집계 완료 - OrderId: {}", event.orderId());

        } catch (Exception e) {
            // 인기 상품 집계 실패해도 결제는 완료됨 (부가 로직)
            log.warn("[이벤트] 인기 상품 집계 실패 - OrderId: {}, Error: {}",
                    event.orderId(), e.getMessage());
        }
    }
}