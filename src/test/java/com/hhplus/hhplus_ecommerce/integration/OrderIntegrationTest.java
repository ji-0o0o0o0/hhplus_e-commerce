package com.hhplus.hhplus_ecommerce.integration;

import com.hhplus.hhplus_ecommerce.cart.application.CartService;
import com.hhplus.hhplus_ecommerce.cart.domain.CartItem;
import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.coupon.application.CouponService;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.application.OrderService;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

public class OrderIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private OrderService orderService;
    @Autowired
    private CartService cartService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;

    private User user;
    private Product product;

    @BeforeEach
    void setup() {
        user = userRepository.save(User.create("테스트 유저"));
        product = productRepository.save(Product.create("테스트 상품", "설명", 10000L, 10, "전자제품"));
    }

    @Test
    @DisplayName("주문 생성 성공")
    void order_생성_성공() {
        // given
        CartItem cartItem = cartService.addCartItem(user.getId(), product.getId(), 2);

        // when
        Order order = orderService.createOrder(user.getId(), List.of(cartItem.getId()), null);

        // then
        assertAll(
                () -> assertThat(order.getUserId()).isEqualTo(user.getId()),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING),
                () -> assertThat(order.getTotalAmount()).isEqualTo(20000L),
                () -> assertThat(order.getDiscountAmount()).isEqualTo(0L),
                () -> assertThat(order.getFinalAmount()).isEqualTo(20000L)
        );

        // 재고 차감 확인
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(8);
    }

    @Test
    @DisplayName("쿠폰 적용 주문 생성 성공")
    void order_쿠폰_적용_주문_생성_성공() {
        // given
        CartItem cartItem = cartService.addCartItem(user.getId(), product.getId(), 2);

        // 20% 할인 쿠폰 생성 및 발급
        Coupon coupon = couponService.createCoupon(
                "20% 할인 쿠폰",
                20,
                10,
                30,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        UserCoupon userCoupon = couponService.issueCoupon(user.getId(), coupon.getId());

        // when
        Order order = orderService.createOrder(user.getId(), List.of(cartItem.getId()), userCoupon.getCouponId());

        // then
        assertAll(
                () -> assertThat(order.getUserId()).isEqualTo(user.getId()),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING),
                () -> assertThat(order.getTotalAmount()).isEqualTo(20000L),
                () -> assertThat(order.getDiscountAmount()).isEqualTo(4000L), // 20% 할인
                () -> assertThat(order.getFinalAmount()).isEqualTo(16000L),
                () -> assertThat(order.getCouponId()).isEqualTo(coupon.getId())
        );
    }

    @Test
    @DisplayName("재고 부족 시 주문 실패")
    void order_재고_부족_시_주문_실패() {
        // given
        // 재고가 10개인 상품
        // 2명의 유저가 각각 6개씩 장바구니에 담음
        User user2 = userRepository.save(User.create("유저2"));

        CartItem cartItem1 = cartService.addCartItem(user.getId(), product.getId(), 6);
        CartItem cartItem2 = cartService.addCartItem(user2.getId(), product.getId(), 6);

        // 첫 번째 유저 주문 성공 (재고 10 → 4)
        orderService.createOrder(user.getId(), List.of(cartItem1.getId()), null);

        // when & then
        // 두 번째 유저 주문 실패 (재고 4개인데 6개 주문)
        assertThatThrownBy(() -> orderService.createOrder(user2.getId(), List.of(cartItem2.getId()), null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_INSUFFICIENT_STOCK);

        // 재고는 첫 번째 주문만 차감되어야 함 (10 - 6 = 4)
        Product productAfterFirstOrder = productRepository.findById(product.getId()).orElseThrow();
        assertThat(productAfterFirstOrder.getStock()).isEqualTo(4);
    }

    @Test
    @DisplayName("빈 장바구니로 주문 실패")
    void order_빈_장바구니로_주문_실패() {
        // when & then
        assertThatThrownBy(() -> orderService.createOrder(user.getId(), List.of(), null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_EMPTY_ITEMS);
    }

    @Test
    @DisplayName("주문 취소 성공")
    void order_주문_취소_성공() {
        // given
        CartItem cartItem = cartService.addCartItem(user.getId(), product.getId(), 3);
        Order order = orderService.createOrder(user.getId(), List.of(cartItem.getId()), null);

        // 주문 후 재고 확인 (10 - 3 = 7)
        Product productAfterOrder = productRepository.findById(product.getId()).orElseThrow();
        assertThat(productAfterOrder.getStock()).isEqualTo(7);

        // when
        orderService.cancelOrder(order.getId());

        // then
        Order cancelledOrder = orderService.getOrder(order.getId());
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // 재고 복구 확인 (7 + 3 = 10)
        Product productAfterCancel = productRepository.findById(product.getId()).orElseThrow();
        assertThat(productAfterCancel.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("완료된 주문 취소 실패")
    void order_완료된_주문_취소_실패() {
        // given
        CartItem cartItem = cartService.addCartItem(user.getId(), product.getId(), 2);
        Order order = orderService.createOrder(user.getId(), List.of(cartItem.getId()), null);

        // 주문 완료 처리
        order.complete();
        orderService.getOrder(order.getId()); // 영속성 컨텍스트에 반영

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(order.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANNOT_CANCEL);
    }

    @Test
    @DisplayName("여러 상품 주문 생성 성공")
    void order_여러_상품_주문_생성_성공() {
        // given
        Product product2 = productRepository.save(Product.create("상품2", "설명2", 5000L, 20, "의류"));

        CartItem cartItem1 = cartService.addCartItem(user.getId(), product.getId(), 2);
        CartItem cartItem2 = cartService.addCartItem(user.getId(), product2.getId(), 3);

        // when
        Order order = orderService.createOrder(
                user.getId(),
                List.of(cartItem1.getId(), cartItem2.getId()),
                null
        );

        // then
        assertAll(
                () -> assertThat(order.getTotalAmount()).isEqualTo(35000L), // (10000*2) + (5000*3)
                () -> assertThat(order.getFinalAmount()).isEqualTo(35000L)
        );

        // 재고 차감 확인
        Product updatedProduct1 = productRepository.findById(product.getId()).orElseThrow();
        Product updatedProduct2 = productRepository.findById(product2.getId()).orElseThrow();
        assertThat(updatedProduct1.getStock()).isEqualTo(8);
        assertThat(updatedProduct2.getStock()).isEqualTo(17);
    }

    @Test
    @DisplayName("사용자별 주문 목록 조회")
    void order_사용자별_주문_목록_조회() {
        // given
        CartItem cartItem1 = cartService.addCartItem(user.getId(), product.getId(), 1);
        CartItem cartItem2 = cartService.addCartItem(user.getId(), product.getId(), 2);

        Order order1 = orderService.createOrder(user.getId(), List.of(cartItem1.getId()), null);
        Order order2 = orderService.createOrder(user.getId(), List.of(cartItem2.getId()), null);

        // 다른 유저의 주문
        User user2 = userRepository.save(User.create("유저2"));
        CartItem cartItem3 = cartService.addCartItem(user2.getId(), product.getId(), 1);
        orderService.createOrder(user2.getId(), List.of(cartItem3.getId()), null);

        // when
        List<Order> userOrders = orderService.getOrdersByUserId(user.getId());

        // then
        assertThat(userOrders).hasSize(2);
        assertThat(userOrders).extracting("id")
                .containsExactlyInAnyOrder(order1.getId(), order2.getId());
    }

    @Test
    @DisplayName("주문 상태별 조회")
    void order_주문_상태별_조회() {
        // given
        CartItem cartItem1 = cartService.addCartItem(user.getId(), product.getId(), 1);
        CartItem cartItem2 = cartService.addCartItem(user.getId(), product.getId(), 1);

        Order order1 = orderService.createOrder(user.getId(), List.of(cartItem1.getId()), null);
        Order order2 = orderService.createOrder(user.getId(), List.of(cartItem2.getId()), null);

        // order1은 취소
        orderService.cancelOrder(order1.getId());

        // when
        List<Order> pendingOrders = orderService.getOrdersByStatus(OrderStatus.PENDING);
        List<Order> cancelledOrders = orderService.getOrdersByStatus(OrderStatus.CANCELLED);

        // then
        assertThat(pendingOrders).extracting("id").contains(order2.getId());
        assertThat(cancelledOrders).extracting("id").contains(order1.getId());
    }
}