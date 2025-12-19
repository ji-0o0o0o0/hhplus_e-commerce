package com.hhplus.hhplus_ecommerce.scheduler;

import com.hhplus.hhplus_ecommerce.cart.domain.CartItem;
import com.hhplus.hhplus_ecommerce.cart.repository.CartItemRepository;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.application.OrderService;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
import com.hhplus.hhplus_ecommerce.order.repository.OrderRepository;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 결제 타임아웃 스케줄러 테스트
 * - 주문 생성 후 일정 시간 경과 시 자동 취소 검증
 * - 재고 복구 검증
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PaymentTimeoutSchedulerTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private EntityManager entityManager;

    private Product testProduct;
    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 999L;

        // 테스트용 상품 생성
        testProduct = Product.builder()
                .name("테스트 상품")
                .price(10000L)
                .stock(100)
                .build();
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("5분 이상 경과한 PENDING 주문이 자동 취소된다")
    void cancelTimeoutOrders_5분초과_자동취소() throws Exception {
        // given - 장바구니에 상품 추가
        CartItem cartItem = CartItem.create(userId, testProduct.getId(), 5);
        cartItem = cartItemRepository.save(cartItem);

        // 주문 생성
        Order order = orderService.createOrderWithDistributedLock(userId, List.of(cartItem.getId()), null);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

        // 주문 생성 시간을 6분 전으로 강제 변경 (Reflection 사용)
        orderRepository.updateCreatedAt(order.getId(), LocalDateTime.now().minusMinutes(6));

        // 재고 확인 - 주문 생성으로 5개 차감됨
        Product productBeforeCancel = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(productBeforeCancel.getStock()).isEqualTo(95);

        // when - 타임아웃 주문 취소 실행 (5분 기준)
        int canceledCount = orderService.cancelTimeoutOrders(5);

        // then
        Order canceledOrder = orderRepository.findById(order.getId()).orElseThrow();
        Product productAfterCancel = productRepository.findById(testProduct.getId()).orElseThrow();

        assertAll(
                () -> assertThat(canceledCount).isEqualTo(1),
                () -> assertThat(canceledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED),
                () -> assertThat(productAfterCancel.getStock()).isEqualTo(100)
        );
    }

    @Test
    @DisplayName("5분 이내의 PENDING 주문은 취소되지 않는다")
    void cancelTimeoutOrders_5분이내_취소안됨() throws Exception {
        // given - 주문 생성 (3분 전)
        CartItem cartItem = CartItem.create(userId, testProduct.getId(), 5);
        cartItem = cartItemRepository.save(cartItem);

        Order order = orderService.createOrderWithDistributedLock(userId, List.of(cartItem.getId()), null);

        // 주문 생성 시간을 3분 전으로 설정
        orderRepository.updateCreatedAt(order.getId(), LocalDateTime.now().minusMinutes(3));

        // when - 타임아웃 주문 취소 실행 (5분 기준)
        int canceledCount = orderService.cancelTimeoutOrders(5);

        // then - 취소되지 않음
        Order stillPendingOrder = orderRepository.findById(order.getId()).orElseThrow();
        Product product = productRepository.findById(testProduct.getId()).orElseThrow();

        assertAll(
                () -> assertThat(canceledCount).isEqualTo(0),
                () -> assertThat(stillPendingOrder.getStatus()).isEqualTo(OrderStatus.PENDING),
                () -> assertThat(product.getStock()).isEqualTo(95)
        );
    }

    @Test
    @DisplayName("COMPLETED 상태의 주문은 타임아웃으로 취소되지 않는다")
    @Transactional
    void cancelTimeoutOrders_완료된주문_취소안됨() throws Exception {
        // given - 주문 생성 및 완료 처리
        CartItem cartItem = CartItem.create(userId, testProduct.getId(), 5);
        cartItem = cartItemRepository.save(cartItem);

        Order order = orderService.createOrderWithDistributedLock(userId, List.of(cartItem.getId()), null);
        order.complete(); // 결제 완료

        // 주문 생성 시간을 10분 전으로 설정
        orderRepository.updateCreatedAt(order.getId(), LocalDateTime.now().minusMinutes(10));

        // when - 타임아웃 주문 취소 실행
        int canceledCount = orderService.cancelTimeoutOrders(5);

        // then - 완료된 주문은 취소되지 않음
        Order completedOrder = orderRepository.findById(order.getId()).orElseThrow();

        assertAll(
                () -> assertThat(canceledCount).isEqualTo(0),
                () -> assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED)
        );
    }

    @Test
    @DisplayName("여러 개의 타임아웃 주문을 일괄 취소한다")
    void cancelTimeoutOrders_복수주문_일괄취소() throws Exception {
        // given - 3개의 타임아웃된 주문 생성
        List<Long> orderIds =new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            CartItem cartItem = CartItem.create(userId + i, testProduct.getId(), 2);
            cartItem = cartItemRepository.save(cartItem);

            Order order = orderService.createOrderWithDistributedLock(userId + i, List.of(cartItem.getId()), null);
            // 모두 6분 전으로 설정
            orderRepository.updateCreatedAt(order.getId(), LocalDateTime.now().minusMinutes(6));
            orderIds.add(order.getId());
        }

        // 재고: 100 - (2 * 3) = 94
        Product productBeforeCancel = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(productBeforeCancel.getStock()).isEqualTo(94);

        // when
        int canceledCount = orderService.cancelTimeoutOrders(5);

        // then - 3건 모두 취소
        Product productAfterCancel = productRepository.findById(testProduct.getId()).orElseThrow();
        List<Order> canceledOrders = orderRepository.findByIdInAndStatus(orderIds ,OrderStatus.CANCELLED);


        assertAll(
                () -> assertThat(canceledCount).isEqualTo(3),
                () -> assertThat(productAfterCancel.getStock()).isEqualTo(100),
                () -> assertThat(canceledOrders).hasSize(3)
        );
    }


}