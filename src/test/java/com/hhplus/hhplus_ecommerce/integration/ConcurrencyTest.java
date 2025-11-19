package com.hhplus.hhplus_ecommerce.integration;

import com.hhplus.hhplus_ecommerce.cart.application.CartService;
import com.hhplus.hhplus_ecommerce.cart.domain.CartItem;
import com.hhplus.hhplus_ecommerce.coupon.application.CouponService;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.order.application.OrderService;
import com.hhplus.hhplus_ecommerce.point.application.PointService;
import com.hhplus.hhplus_ecommerce.point.domain.Point;
import com.hhplus.hhplus_ecommerce.point.repository.PointRepository;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import com.hhplus.hhplus_ecommerce.user.domain.User;
import com.hhplus.hhplus_ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

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
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CartService cartService;

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

     @Test
     @DisplayName("쿠폰 발급 동시성 테스트 - 100명이 10개 쿠폰 발급")
     void 쿠폰_발급_동시성_테스트() throws InterruptedException {
         // given
         // 사전에 100명의 유저 생성
         User[] users = new User[100];
         for (int i = 0; i < 100; i++) {
             users[i] = userRepository.save(User.create("유저" + i));
         }

         Coupon coupon = couponService.createCoupon(
                 "선착순 쿠폰",
                 20,
                 10,  // 총 10개만 발급 가능
                 30,
                 LocalDateTime.now().minusDays(1),
                 LocalDateTime.now().plusDays(30)
         );
        couponRepository.save(coupon);

         int threadCount = 100;  // 100명이 동시에 발급 시도
         ExecutorService executorService = Executors.newFixedThreadPool(32);
         CountDownLatch latch = new CountDownLatch(threadCount);

         AtomicInteger successCount = new AtomicInteger(0);
         AtomicInteger failCount = new AtomicInteger(0);

         // when
         for (int i = 0; i < threadCount; i++) {
             final int userIndex = i;
             executorService.submit(() -> {
                 try {
                     couponService.issueCoupon(users[userIndex].getId(), coupon.getId());
                     successCount.incrementAndGet();
                 } catch (Exception e) {
                     failCount.incrementAndGet();
                     log.error("쿠폰 발급 실패: {}", e.getMessage(), e);
                 } finally {
                     latch.countDown();
                 }
             });
         }

         latch.await();
         executorService.shutdown();

         // then
         Coupon finalCoupon = couponRepository.findById(coupon.getId()).orElseThrow();

         log.info("=== 쿠폰 발급 동시성 테스트 결과 ===");
         log.info("성공 횟수: {}", successCount.get());
         log.info("실패 횟수: {}", failCount.get());
         log.info("최종 발급 수량: {}", finalCoupon.getIssuedQuantity());
         log.info("총 수량: {}", finalCoupon.getTotalQuantity());

         // 정확히 10개만 발급되어야 함
         assertAll(
                 () -> assertThat(finalCoupon.getIssuedQuantity()).isEqualTo(10),
                 () -> assertThat(successCount.get()).isEqualTo(10),
                 () -> assertThat(failCount.get()).isEqualTo(90)
         );
     }

    @Test
    @DisplayName("재고 차감 동시성 테스트 - 10명이 재고 5개 상품 구매")
    void 재고_차감_동시성_테스트() throws InterruptedException {
        // given
        // 재고 5개인 상품 생성
        Product product = productRepository.save(Product.create("한정 상품", "재고 5개", 10000L, 5, "전자제품"));

        // 10명의 유저 생성 (각각 1개씩 구매 시도)
        User[] users = new User[10];
        for (int i = 0; i < 10; i++) {
            users[i] = userRepository.save(User.create("구매유저" + i));
            pointRepository.save(Point.create(users[i].getId()));
        }

        int threadCount = 10;  // 10명이 동시에 주문
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    // 장바구니에 상품 담기
                    CartItem cartItem = cartService.addCartItem(users[userIndex].getId(), product.getId(), 1);

                    // 주문 생성 (재고 차감)
                    orderService.createOrder(users[userIndex].getId(), List.of(cartItem.getId()), null);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.warn("주문 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Product finalProduct = productRepository.findById(product.getId()).orElseThrow();

        log.info("=== 재고 차감 동시성 테스트 결과 ===");
        log.info("성공 횟수: {}", successCount.get());
        log.info("실패 횟수: {}", failCount.get());
        log.info("최종 재고: {}", finalProduct.getStock());
        log.info("초기 재고: 5");

        // 정확히 5명만 성공해야 함 (재고 5개)
        assertAll(
                () ->assertThat(finalProduct.getStock()).isEqualTo(0),
                () ->assertThat(successCount.get()).isEqualTo(5),
                () -> assertThat(failCount.get()).isEqualTo(5)
        );
    }

}