package com.hhplus.hhplus_ecommerce.scheduler;

import com.hhplus.hhplus_ecommerce.coupon.application.CouponRedisService;
import com.hhplus.hhplus_ecommerce.coupon.repository.RedisCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CouponIssueScheduler {
    private static final int BATCH_SIZE = 100;  // 한 번에 처리할 대기열 크기

    private final CouponRedisService couponRedisService;
    private final RedisCouponRepository redisCouponRepository;

   
    @Scheduled(fixedDelay = 1000)  // 1초마다
    public void processCouponQueues() {
        // Redis에서 대기열이 있는 쿠폰 ID 가져오기
        List<Long> couponIdsWithQueue = redisCouponRepository.getCouponIdsWithQueue();

        if (couponIdsWithQueue.isEmpty()) {
            return;  // 대기열 없으면 스킵
        }

        log.debug("[스케줄러] 처리 대상 쿠폰: {}", couponIdsWithQueue);

        for (Long couponId : couponIdsWithQueue) {
            try {
                int processed = couponRedisService.processCouponQueue(couponId, BATCH_SIZE);
                if (processed > 0) {
                    log.info("[스케줄러] 쿠폰 대기열 처리 완료: couponId={}, processed={}", couponId, processed);
                }
            } catch (Exception e) {
                log.warn("[스케줄러] 쿠폰 대기열 처리 실패: couponId={}", couponId, e);
            }
        }
    }
}
