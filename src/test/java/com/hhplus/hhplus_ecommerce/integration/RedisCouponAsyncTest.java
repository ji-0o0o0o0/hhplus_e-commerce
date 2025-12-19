package com.hhplus.hhplus_ecommerce.integration;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.coupon.application.CouponRedisService;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.coupon.repository.RedisCouponRepository;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestRedisConfig.class)
@DisplayName("Redis 기반 비동기 쿠폰 발급 테스트")
class RedisCouponAsyncTest extends BaseIntegrationTest {

    @Autowired
    private CouponRedisService couponRedisService;

    @Autowired
    private RedisCouponRepository redisCouponRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        // 테스트용 쿠폰 생성
        testCoupon = Coupon.create(
                "비동기 쿠폰",
                10,
                100, // 총 100개
                10,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(10)
        );
        testCoupon = couponRepository.save(testCoupon);

        // Redis 재고 초기화
        couponRedisService.initializeCouponStock(testCoupon.getId());
    }

    @AfterEach
    void tearDown() {
        // Redis 데이터 정리
        redisCouponRepository.clear(testCoupon.getId());
    }

    @Test
    @DisplayName("비동기 쿠폰 발급 - 대기열 추가 후 스케줄러가 처리")
    void asyncCouponIssue_QueueAndProcess() throws InterruptedException {
        // given
        int requestCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch latch = new CountDownLatch(requestCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 50명이 동시에 대기열에 요청
        for (int i = 1; i <= requestCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    couponRedisService.requestCouponAsync(userId, testCoupon.getId());
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 모두 대기열에 추가 성공
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(couponRedisService.getQueueSize(testCoupon.getId())).isEqualTo(50L);

        // when: 스케줄러가 대기열 처리 (10개씩 5번)
        int processed = 0;
        for (int i = 0; i < 5; i++) {
            processed += couponRedisService.processCouponQueue(testCoupon.getId(), 10);
        }

        // then: 50개 모두 처리 완료
        assertThat(processed).isEqualTo(50);
        assertThat(couponRedisService.getQueueSize(testCoupon.getId())).isEqualTo(0L);

        // DB 확인
        List<UserCoupon> userCoupons = userCouponRepository.findAll();
        assertThat(userCoupons).hasSize(50);
    }

    @Test
    @DisplayName("비동기 쿠폰 발급 - 200명 요청, 100개 재고, 정확히 100명만 발급")
    void asyncCouponIssue_200Users100Stock() throws InterruptedException {
        // given
        int requestCount = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch latch = new CountDownLatch(requestCount);

        AtomicInteger queueSuccessCount = new AtomicInteger(0);
        AtomicInteger queueFailCount = new AtomicInteger(0);

        // when: 200명이 동시에 대기열에 요청
        for (int i = 1; i <= requestCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    couponRedisService.requestCouponAsync(userId, testCoupon.getId());
                    queueSuccessCount.incrementAndGet();
                } catch (BusinessException e) {
                    queueFailCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // 대기열 추가 성공 확인
        Long queueSize = couponRedisService.getQueueSize(testCoupon.getId());
        assertThat(queueSuccessCount.get()).isLessThanOrEqualTo(100);  // 재고 확인으로 100명 이하만 성공

        // when: 스케줄러가 대기열 처리
        int totalProcessed = 0;
        while (couponRedisService.getQueueSize(testCoupon.getId()) > 0) {
            totalProcessed += couponRedisService.processCouponQueue(testCoupon.getId(), 20);
        }

        // then: 정확히 100명만 발급
        List<UserCoupon> userCoupons = userCouponRepository.findAll();
        assertThat(userCoupons).hasSize(100);
        // assertThat(couponRedisService.getCurrentStock(testCoupon.getId())).isEqualTo(0L);
    }

    @Test
    @DisplayName("중복 발급 방지 - 같은 사용자가 여러 번 요청해도 1번만 대기열 추가")
    void preventDuplicateQueue_SameUserMultipleRequests() throws InterruptedException {
        // given
        Long userId = 1L;
        int requestCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch latch = new CountDownLatch(requestCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                try {
                    couponRedisService.requestCouponAsync(userId, testCoupon.getId());
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 1번만 성공
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(9);
        assertThat(couponRedisService.getQueueSize(testCoupon.getId())).isEqualTo(1L);

        // when: 스케줄러 처리
        couponRedisService.processCouponQueue(testCoupon.getId(), 10);

        // then: 1개만 발급
        List<UserCoupon> userCoupons = userCouponRepository.findAll();
        assertThat(userCoupons).hasSize(1);
    }

    @Test
    @DisplayName("대기열 Bulk 처리 - 한 번에 여러 요청 처리")
    void bulkProcessQueue() {
        // given: 100명 요청
        for (int i = 1; i <= 100; i++) {
            couponRedisService.requestCouponAsync((long) i, testCoupon.getId());
        }

        assertThat(couponRedisService.getQueueSize(testCoupon.getId())).isEqualTo(100L);

        // when: 50개씩 2번 처리
        int processed1 = couponRedisService.processCouponQueue(testCoupon.getId(), 50);
        int processed2 = couponRedisService.processCouponQueue(testCoupon.getId(), 50);

        // then
        assertThat(processed1).isEqualTo(50);
        assertThat(processed2).isEqualTo(50);
        assertThat(couponRedisService.getQueueSize(testCoupon.getId())).isEqualTo(0L);

        // DB 확인
        List<UserCoupon> userCoupons = userCouponRepository.findAll();
        assertThat(userCoupons).hasSize(100);
    }
}