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
@DisplayName("Redis 기반 선착순 쿠폰 발급 동시성 테스트")
class RedisCouponConcurrencyTest extends BaseIntegrationTest {

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
                "선착순 쿠폰",
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
    @DisplayName("동시에 100명이 100개 쿠폰 발급 요청 - 정확히 100명만 성공")
    void concurrentCouponIssue_100Users100Stock_Exactly100Success() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    couponRedisService.issueCouponWithRedis(userId, testCoupon.getId());
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

        // then
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(0);
        // assertThat(couponRedisService.getCurrentStock(testCoupon.getId())).isEqualTo(0L);
        assertThat(couponRedisService.getIssuedCount(testCoupon.getId())).isEqualTo(100L);
    }

    @Test
    @DisplayName("동시에 200명이 100개 쿠폰 발급 요청 - 정확히 100명만 성공, 100명 실패")
    void concurrentCouponIssue_200Users100Stock_100Success100Fail() throws InterruptedException {
        // given
        int threadCount = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    couponRedisService.issueCouponWithRedis(userId, testCoupon.getId());
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

        // then
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(100);
        // assertThat(couponRedisService.getCurrentStock(testCoupon.getId())).isEqualTo(0L);
        assertThat(couponRedisService.getIssuedCount(testCoupon.getId())).isEqualTo(100L);
    }

    @Test
    @DisplayName("중복 발급 방지 - 같은 사용자가 여러 번 요청해도 1번만 성공")
    void preventDuplicateIssue_SameUserMultipleRequests() throws InterruptedException {
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
                    couponRedisService.issueCouponWithRedis(userId, testCoupon.getId());
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

        // then
        assertThat(successCount.get()).isEqualTo(1);  // 1번만 성공
        assertThat(failCount.get()).isEqualTo(9);     // 9번 실패
        // assertThat(couponRedisService.getCurrentStock(testCoupon.getId())).isEqualTo(99L);
    }

    @Test
    @DisplayName("DB와 Redis 정합성 확인 - 발급된 쿠폰 수 일치")
    void checkDataConsistency_RedisAndDB() throws InterruptedException {
        // given
        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    couponRedisService.issueCouponWithRedis(userId, testCoupon.getId());
                } catch (BusinessException ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Long redisIssuedCount = couponRedisService.getIssuedCount(testCoupon.getId());
        List<UserCoupon> dbUserCoupons = userCouponRepository.findAll();

        assertThat(redisIssuedCount).isEqualTo((long) dbUserCoupons.size());
        assertThat(dbUserCoupons).hasSize(50);
    }

    @Test
    @DisplayName("재고 0 이후 발급 시도 - 모두 실패")
    void issueAfterStockOut_AllFail() throws InterruptedException {
        // given: 먼저 100개 모두 발급
        for (int i = 1; i <= 100; i++) {
            couponRedisService.issueCouponWithRedis((long) i, testCoupon.getId());
        }
        // assertThat(couponRedisService.getCurrentStock(testCoupon.getId())).isEqualTo(0L);

        // when: 추가로 10명이 발급 시도
        int additionalUsers = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(additionalUsers);
        CountDownLatch latch = new CountDownLatch(additionalUsers);

        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 101; i <= 110; i++) {
            final long userId = i;
            executorService.submit(() -> {
                try {
                    couponRedisService.issueCouponWithRedis(userId, testCoupon.getId());
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        assertThat(failCount.get()).isEqualTo(10);  // 모두 실패
        // assertThat(couponRedisService.getCurrentStock(testCoupon.getId())).isEqualTo(0L);
        assertThat(couponRedisService.getIssuedCount(testCoupon.getId())).isEqualTo(100L);
    }
}