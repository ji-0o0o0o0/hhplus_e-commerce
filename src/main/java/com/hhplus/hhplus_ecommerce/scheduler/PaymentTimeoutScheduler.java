package com.hhplus.hhplus_ecommerce.scheduler;

import com.hhplus.hhplus_ecommerce.order.application.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 결제 타임아웃 스케줄러
 * - 매 1분마다 PENDING 상태의 주문 중 5분 이상 경과한 주문을 자동 취소
 * - 재고 자동 복구
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {

    private final OrderService orderService;
    private static final int PAYMENT_TIMEOUT_MINUTES = 5;

    /**
     * 결제 타임아웃 주문 자동 취소 (매 1분마다 실행)
     * - cron: 매 1분마다 실행
     * - 주문 생성 후 5분 이내에 결제하지 않으면 자동 취소
     */
    @Scheduled(cron = "0 * * * * *") // 매 분 0초에 실행
    public void cancelTimeoutOrders() {
        try {
            log.debug("[결제 타임아웃 스케줄러] 타임아웃 주문 검사 시작 (타임아웃 기준: {}분)", PAYMENT_TIMEOUT_MINUTES);

            int canceledCount = orderService.cancelTimeoutOrders(PAYMENT_TIMEOUT_MINUTES);

            if (canceledCount > 0) {
                log.info("[결제 타임아웃 스케줄러] {}건의 주문 자동 취소 완료", canceledCount);
            }

        } catch (Exception e) {
            log.error("[결제 타임아웃 스케줄러] 스케줄러 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
