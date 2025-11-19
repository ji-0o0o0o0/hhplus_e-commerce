package com.hhplus.hhplus_ecommerce.integration;

import com.hhplus.hhplus_ecommerce.cart.application.CartService;
import com.hhplus.hhplus_ecommerce.cart.domain.CartItem;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.application.CouponService;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.application.OrderService;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
import com.hhplus.hhplus_ecommerce.payment.application.PaymentService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 전체 주문 플로우 통합 테스트
 * - 포인트 충전 → 상품 조회 → 장바구니 담기 → 쿠폰 발급 → 주문 생성 → 결제 → 검증
 */
public class FullOrderFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PointService pointService;
    @Autowired
    private PointRepository pointRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private UserCouponRepository userCouponRepository;

    private User user;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setup() {
        user = userRepository.save(User.create("통합테스트 사용자"));
        pointRepository.save(Point.create(user.getId()));

        product1 = productRepository.save(Product.create("노트북", "고성능 노트북", 1000000L, 10, "전자제품"));
        product2 = productRepository.save(Product.create("마우스", "무선 마우스", 50000L, 20, "전자제품"));
    }

    @Test
    @DisplayName("전체 주문 플로우 - 쿠폰 없이")
    void 전체_주문_플로우_쿠폰_없이() {
        // 1. 포인트 충전 (1회 최대 100만원이므로 2번 충전)
        pointService.changePoint(user.getId(), 1000000L);
        pointService.changePoint(user.getId(), 1000000L);
        Point point = pointRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(point.getAmount()).isEqualTo(2000000L);

        // 2. 장바구니에 상품 담기
        CartItem cartItem1 = cartService.addCartItem(user.getId(), product1.getId(), 1);
        CartItem cartItem2 = cartService.addCartItem(user.getId(), product2.getId(), 2);

        // 3. 주문 생성
        Order order = orderService.createOrder(
                user.getId(),
                List.of(cartItem1.getId(), cartItem2.getId()),
                null
        );

        // 주문 검증
        assertAll(
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING),
                () -> assertThat(order.getTotalAmount()).isEqualTo(1100000L), // 1,000,000 + 100,000
                () -> assertThat(order.getDiscountAmount()).isEqualTo(0L),
                () -> assertThat(order.getFinalAmount()).isEqualTo(1100000L)
        );

        // 재고 차감 확인
        Product updatedProduct1 = productRepository.findById(product1.getId()).orElseThrow();
        Product updatedProduct2 = productRepository.findById(product2.getId()).orElseThrow();
        assertThat(updatedProduct1.getStock()).isEqualTo(9); // 10 - 1
        assertThat(updatedProduct2.getStock()).isEqualTo(18); // 20 - 2

        // 4. 결제 처리
        paymentService.executePayment(user.getId(), order.getId());

        // 결제 후 주문 상태 확인
        Order completedOrder = orderService.getOrder(order.getId());
        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        // 포인트 차감 확인
        Point updatedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(updatedPoint.getAmount()).isEqualTo(900000L); // 2,000,000 - 1,100,000
    }

    @Test
    @DisplayName("전체 주문 플로우 - 쿠폰 적용")
    void 전체_주문_플로우_쿠폰_적용() {
        // 1. 포인트 충전 (1회 최대 100만원이므로 2번 충전)
        pointService.changePoint(user.getId(), 1000000L);
        pointService.changePoint(user.getId(), 1000000L);

        // 2. 쿠폰 생성 및 발급
        Coupon coupon = couponService.createCoupon(
                "20% 할인 쿠폰",
                20,
                100,
                30,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        UserCoupon userCoupon = couponService.issueCoupon(user.getId(), coupon.getId());

        assertAll(
                () -> assertThat(userCoupon.getStatus()).isEqualTo(CouponStatus.AVAILABLE),
                () -> assertThat(userCoupon.getDiscountRate()).isEqualTo(20)
        );

        // 3. 장바구니에 상품 담기
        CartItem cartItem = cartService.addCartItem(user.getId(), product1.getId(), 1);

        // 4. 주문 생성 (쿠폰 적용)
        Order order = orderService.createOrder(
                user.getId(),
                List.of(cartItem.getId()),
                userCoupon.getCouponId()
        );

        // 주문 검증
        assertAll(
                () -> assertThat(order.getTotalAmount()).isEqualTo(1000000L),
                () -> assertThat(order.getDiscountAmount()).isEqualTo(200000L), // 20% 할인
                () -> assertThat(order.getFinalAmount()).isEqualTo(800000L),
                () -> assertThat(order.getCouponId()).isEqualTo(coupon.getId())
        );

        // 5. 결제 처리
        paymentService.executePayment(user.getId(), order.getId());

        // 결제 후 검증
        Order completedOrder = orderService.getOrder(order.getId());
        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        // 쿠폰 사용 확인
        UserCoupon usedCoupon = userCouponRepository.findByUserIdAndCouponId(user.getId(), coupon.getId())
                .orElseThrow();
        assertThat(usedCoupon.getStatus()).isEqualTo(CouponStatus.USED);
        assertThat(usedCoupon.getUsedAt()).isNotNull();

        // 포인트 차감 확인
        Point updatedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(updatedPoint.getAmount()).isEqualTo(1200000L); // 2,000,000 - 800,000
    }

    @Test
    @DisplayName("전체 주문 플로우 - 다중 상품 + 쿠폰 적용")
    void 전체_주문_플로우_다중_상품_쿠폰_적용() {
        // 1. 포인트 충전 (1회 최대 100만원이므로 3번 충전)
        pointService.changePoint(user.getId(), 1000000L);
        pointService.changePoint(user.getId(), 1000000L);
        pointService.changePoint(user.getId(), 1000000L);

        // 2. 쿠폰 발급
        Coupon coupon = couponService.createCoupon(
                "10% 할인 쿠폰",
                10,
                50,
                30,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        UserCoupon userCoupon = couponService.issueCoupon(user.getId(), coupon.getId());

        // 3. 장바구니에 여러 상품 담기
        CartItem cartItem1 = cartService.addCartItem(user.getId(), product1.getId(), 2); // 2,000,000
        CartItem cartItem2 = cartService.addCartItem(user.getId(), product2.getId(), 3); // 150,000

        // 4. 주문 생성
        Order order = orderService.createOrder(
                user.getId(),
                List.of(cartItem1.getId(), cartItem2.getId()),
                userCoupon.getCouponId()
        );

        // 주문 검증
        assertAll(
                () -> assertThat(order.getTotalAmount()).isEqualTo(2150000L), // 2,000,000 + 150,000
                () -> assertThat(order.getDiscountAmount()).isEqualTo(215000L), // 10% 할인
                () -> assertThat(order.getFinalAmount()).isEqualTo(1935000L),
                () -> assertThat(order.getItems()).hasSize(2)
        );

        // 5. 결제 처리
        paymentService.executePayment(user.getId(), order.getId());

        // 최종 검증
        Order completedOrder = orderService.getOrder(order.getId());
        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        Point finalPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(finalPoint.getAmount()).isEqualTo(1065000L); // 3,000,000 - 1,935,000

        Product finalProduct1 = productRepository.findById(product1.getId()).orElseThrow();
        Product finalProduct2 = productRepository.findById(product2.getId()).orElseThrow();
        assertThat(finalProduct1.getStock()).isEqualTo(8); // 10 - 2
        assertThat(finalProduct2.getStock()).isEqualTo(17); // 20 - 3
    }
}