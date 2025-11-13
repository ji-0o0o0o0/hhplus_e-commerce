package com.hhplus.hhplus_ecommerce.integration;

import com.hhplus.hhplus_ecommerce.coupon.application.CouponService;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.point.application.PointService;
import com.hhplus.hhplus_ecommerce.point.domain.Point;
import com.hhplus.hhplus_ecommerce.point.repository.PointRepository;
import com.hhplus.hhplus_ecommerce.user.domain.User;
import com.hhplus.hhplus_ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrencyTest extends BaseConcurrencyTest {
    @Autowired
    private PointService pointService;
    @Autowired
    private PointRepository pointRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CouponService couponService;
    @Autowired
    private CouponRepository couponRepository;

    private User user;

    @BeforeEach
    void setup() {
        user = userRepository.save(User.create("동시성 테스트 유저"));
        pointRepository.save(Point.create(user.getId()));
    }

    @Test
    @DisplayName("포인트 충전 동시성 테스트 - 100개 스레드")
    void 포인트_충전_동시성_테스트() throws InterruptedException {
        // given
        int threadCount = 100;
        long chargeAmount = 1000L;  // 각 스레드당 1000원 충전
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.changePoint(user.getId(), chargeAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.warn("충전 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Point finalPoint = pointRepository.findByUserId(user.getId()).orElseThrow();

        log.info("=== 포인트 충전 동시성 테스트 결과 ===");
        log.info("성공 횟수: {}", successCount.get());
        log.info("실패 횟수: {}", failCount.get());
        log.info("최종 포인트: {}", finalPoint.getAmount());
        log.info("예상 포인트: {}", chargeAmount * threadCount);

        // 낙관적 락으로 재시도하므로 모두 성공해야 함
        assertThat(finalPoint.getAmount()).isEqualTo(chargeAmount * threadCount);
    }

    
}