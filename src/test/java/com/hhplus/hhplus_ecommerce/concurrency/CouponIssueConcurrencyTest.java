package com.hhplus.hhplus_ecommerce.concurrency;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.coupon.application.CouponService;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 선착순 쿠폰 발급 동시성 테스트
 * - 100명이 동시에 50개 한정 쿠폰을 신청하는 시나리오
 * - Race Condition 방지 검증
 */
class CouponIssueConcurrencyTest {

    private CouponService couponService;
    private InMemoryCouponRepository couponRepository;
    private InMemoryUserCouponRepository userCouponRepository;
    private com.hhplus.hhplus_ecommerce.common.lock.LockManager lockManager;

    @BeforeEach
    void setUp() {
        couponRepository = new InMemoryCouponRepository();
        userCouponRepository = new InMemoryUserCouponRepository();
        lockManager = new com.hhplus.hhplus_ecommerce.common.lock.LockManager();
        couponService = new CouponService(couponRepository, userCouponRepository, lockManager);
    }

    @Test
    @DisplayName("100명이 동시에 50개 한정 쿠폰 발급 요청 시 정확히 50명만 발급받는다")
    void concurrentCouponIssue_shouldIssueExactly50Coupons() throws InterruptedException {
        // Given: 50개 한정 쿠폰 생성
        Coupon coupon = couponService.createCoupon(
                "선착순 쿠폰",
                10,
                50,
                30,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7)
        );

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 100명이 동시에 쿠폰 발급 요청
        for (int i = 0; i < threadCount; i++) {
            long userId = i + 1;
            executorService.submit(() -> {
                try {
                    couponService.issueCoupon(userId, coupon.getId());
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

        // Then: 정확히 50명만 성공
        assertEquals(50, successCount.get(), "성공 카운트가 50이어야 함");
        assertEquals(50, failCount.get(), "실패 카운트가 50이어야 함");

        // 실제 발급된 쿠폰 확인
        Coupon updatedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertEquals(50, updatedCoupon.getIssuedQuantity(), "발급된 수량이 정확히 50개여야 함");
        assertFalse(updatedCoupon.canIssue(), "더 이상 발급 불가능해야 함");
    }

    @Test
    @DisplayName("1000명이 동시에 100개 한정 쿠폰 발급 요청 시 정확히 100명만 발급받는다")
    void concurrentCouponIssue_highLoad_shouldIssueExactly100Coupons() throws InterruptedException {
        // Given: 100개 한정 쿠폰 생성
        Coupon coupon = couponService.createCoupon(
                "대용량 선착순 쿠폰",
                15,
                100,
                30,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7)
        );

        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 1000명이 동시에 쿠폰 발급 요청
        for (int i = 0; i < threadCount; i++) {
            long userId = i + 1;
            executorService.submit(() -> {
                try {
                    couponService.issueCoupon(userId, coupon.getId());
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

        // Then: 정확히 100명만 성공
        assertEquals(100, successCount.get(), "성공 카운트가 100이어야 함");
        assertEquals(900, failCount.get(), "실패 카운트가 900이어야 함");

        // 실제 발급된 쿠폰 확인
        Coupon updatedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertEquals(100, updatedCoupon.getIssuedQuantity(), "발급된 수량이 정확히 100개여야 함");
    }

    @Test
    @DisplayName("동일 사용자가 동일 쿠폰을 여러 번 발급 시도해도 1개만 발급된다")
    void concurrentCouponIssue_sameUser_shouldIssuedOnlyOnce() throws InterruptedException {
        // Given: 충분한 수량의 쿠폰 생성
        Coupon coupon = couponService.createCoupon(
                "중복 발급 방지 쿠폰",
                10,
                100,
                30,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7)
        );

        int threadCount = 10;
        long userId = 1L;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When: 동일 사용자가 10번 동시 발급 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    couponService.issueCoupon(userId, coupon.getId());
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    // 중복 발급 예외는 정상
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 정확히 1번만 성공
        assertEquals(1, successCount.get(), "동일 사용자는 1번만 발급 성공해야 함");

        // 실제 발급된 쿠폰 확인
        Coupon updatedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertEquals(1, updatedCoupon.getIssuedQuantity(), "발급된 수량이 1개여야 함");
    }
}
