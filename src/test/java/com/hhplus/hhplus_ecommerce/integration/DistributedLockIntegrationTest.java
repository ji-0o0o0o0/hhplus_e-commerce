package com.hhplus.hhplus_ecommerce.integration;

import com.hhplus.hhplus_ecommerce.cart.domain.CartItem;
import com.hhplus.hhplus_ecommerce.cart.repository.CartItemRepository;
import com.hhplus.hhplus_ecommerce.coupon.application.CouponService;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.point.application.PointService;
import com.hhplus.hhplus_ecommerce.point.domain.Point;
import com.hhplus.hhplus_ecommerce.point.repository.PointRepository;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import com.hhplus.hhplus_ecommerce.order.application.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@Testcontainers
@Import(TestRedisConfig.class)
@org.springframework.test.context.ActiveProfiles("test")
class DistributedLockIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private PointService pointService;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @BeforeEach
    void setUp() {
        pointRepository.deleteAll();
        couponRepository.deleteAll();
        productRepository.deleteAll();
        cartItemRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        pointRepository.deleteAll();
        couponRepository.deleteAll();
        productRepository.deleteAll();
        cartItemRepository.deleteAll();
    }

    @Test
    @DisplayName("분산락 - 동시 포인트 충전 요청 시 정확한 금액이 충전된다")
    void concurrentPointCharge_WithDistributedLock() throws InterruptedException {
        // Given
        Long userId = 1L;
        Long chargeAmount = 1000L;
        int threadCount = 10;

        // When
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePointWithDistributedLock(userId, chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        Point point = pointService.getPoint(userId);
        assertAll(
                () -> assertThat(point.getAmount()).isEqualTo(chargeAmount * threadCount)
        );
    }

    @Test
    @DisplayName("분산락 - 동시 쿠폰 발급 요청 시 수량 제한이 정확히 지켜진다")
    void concurrentCouponIssue_WithDistributedLock() throws InterruptedException {
        // Given
        Coupon coupon = couponService.createCoupon(
                "한정 쿠폰",
                10,
                50, // 총 50개만 발급 가능
                30,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        int threadCount = 100; // 100명이 동시에 신청
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1L;
            executorService.submit(() -> {
                try {
                    couponService.issueCouponWithDistributedLock(userId, coupon.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        Coupon updatedCoupon = couponService.getCoupon(coupon.getId());
        assertAll(
                () -> assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(50),
                () -> assertThat(successCount.get()).isEqualTo(50),
                () -> assertThat(failCount.get()).isEqualTo(50)
        );
    }

    @Test
    @DisplayName("분산락 - 동시 재고 차감 요청 시 재고가 정확히 차감된다")
    void concurrentStockDecrease_WithDistributedLock() throws InterruptedException {
        // Given
        Product product = Product.create("테스트 상품", "분산락 테스트용 상품", 10000L, 100, "테스트");
        productRepository.save(product);

        int threadCount = 50;
        int orderQuantity = 1;

        // 장바구니 아이템들 미리 생성
        List<Long> cartItemIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Long userId = (long) (i + 1);
            CartItem cartItem = CartItem.create(userId, product.getId(), orderQuantity);
            cartItemRepository.save(cartItem);
            cartItemIds.add(cartItem.getId());
        }

        // When
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            final Long userId = (long) (index + 1);
            executorService.submit(() -> {
                try {
                    orderService.createOrderWithDistributedLock(
                            userId,
                            List.of(cartItemIds.get(index)),
                            null
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        int expectedSuccessCount = threadCount - failCount.get();
        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(expectedSuccessCount),
                () -> assertThat(updatedProduct.getStock()).isEqualTo(100 - expectedSuccessCount),
                () -> assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount)
        );
    }

    @Test
    @DisplayName("분산락 - 동시 포인트 충전과 사용 시 정확한 금액이 유지된다")
    void concurrentPointChargeAndUse_WithDistributedLock() throws InterruptedException {
        // Given
        Long userId = 1L;
        Long chargeAmount = 1000L;
        Long useAmount = 500L;
        int chargeCount = 10;
        int useCount = 5;

        // When
        ExecutorService executorService = Executors.newFixedThreadPool(chargeCount + useCount);
        CountDownLatch latch = new CountDownLatch(chargeCount + useCount);

        // 충전 10번
        for (int i = 0; i < chargeCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePointWithDistributedLock(userId, chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 사용 5번
        for (int i = 0; i < useCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.usePointWithDistributedLock(userId, useAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        Point point = pointService.getPoint(userId);
        long expectedAmount = (chargeAmount * chargeCount) - (useAmount * useCount);
        assertAll(
                () -> assertThat(point.getAmount()).isEqualTo(expectedAmount)  // 10000 - 2500 = 7500
        );
    }

    @Test
    @DisplayName("분산락 - 락 순차 처리 테스트")
    void lockSequentialProcessing_Test() throws InterruptedException {
        // Given
        Long userId = 1L;
        Long chargeAmount = 1000L;

        // When
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // 첫 번째 스레드: 락을 먼저 획득
        executorService.submit(() -> {
            try {
                pointService.chargePointWithDistributedLock(userId, chargeAmount);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        Thread.sleep(100); // 첫 번째 요청이 락을 먼저 획득하도록 대기

        // 두 번째 스레드: 락 대기 후 획득
        executorService.submit(() -> {
            try {
                pointService.chargePointWithDistributedLock(userId, chargeAmount);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executorService.shutdown();

        // Then
        Point point = pointService.getPoint(userId);
        assertAll(
                () -> assertThat(successCount.get()).isEqualTo(2),  // 순차적으로 처리되어 모두 성공
                () -> assertThat(point.getAmount()).isEqualTo(chargeAmount * 2)
        );
    }
}
