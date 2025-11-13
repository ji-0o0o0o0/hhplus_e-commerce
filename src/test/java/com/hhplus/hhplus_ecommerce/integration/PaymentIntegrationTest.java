package com.hhplus.hhplus_ecommerce.integration;

import com.hhplus.hhplus_ecommerce.cart.application.CartService;
import com.hhplus.hhplus_ecommerce.cart.domain.CartItem;
import com.hhplus.hhplus_ecommerce.cart.repository.CartItemRepository;
import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.application.OrderService;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
import com.hhplus.hhplus_ecommerce.order.repository.OrderRepository;
import com.hhplus.hhplus_ecommerce.payment.application.PaymentService;
import com.hhplus.hhplus_ecommerce.point.application.PointService;
import com.hhplus.hhplus_ecommerce.point.domain.Point;
import com.hhplus.hhplus_ecommerce.point.repository.PointRepository;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import com.hhplus.hhplus_ecommerce.user.domain.User;
import com.hhplus.hhplus_ecommerce.user.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

public class PaymentIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PointRepository pointRepository;
    @Autowired
    private PointService pointService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentService paymentService;

    private User user;
    private Product product;

    @BeforeEach
    void setup() {
        user = userRepository.save(User.create("테스트 유저"));
        //포인트 초기화
        pointRepository.save(Point.create(user.getId()));
        product = productRepository.save(Product.create("테스트 상품","설명",10000L,10,"전자제품"));
    }

    @Test
    @DisplayName("주문 생성 후 결제까지 정상 처리")
    void payment_결재정상처리(){
        //given
        //충전
        pointService.changePoint(user.getId(),50000L);

        //장바구니 추가
        CartItem cartItem = cartService.addCartItem(user.getId(), product.getId(),2);

        //주문생성
        Order order = orderService.createOrder(user.getId(), List.of(cartItem.getId()),null);

        assertAll(
                () -> Assertions.assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING),
                () -> Assertions.assertThat(order.getFinalAmount()).isEqualTo(20000L)
        );

        //when
        //결제 진행
        paymentService.executePayment(user.getId(),order.getId());

        //then
        Order completedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        Point updatedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(updatedPoint.getAmount()).isEqualTo(30000L);

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(8);

    }

    @Test
    @DisplayName("포인트가 부족하면 결제가 실패")
    void payment_포인트가_부족하면_결제실패(){
        //given
        // 필요: 20,000원 (10,000 * 2)
        // 충전: 15,000원 (부족)
        pointService.changePoint(user.getId(), 15000L);

        CartItem cartItem = cartService.addCartItem(user.getId(), product.getId(),2);
        Order order = orderService.createOrder(user.getId(), List.of(cartItem.getId()),null);

        //when,then
        assertThatThrownBy(() -> paymentService.executePayment(user.getId(), order.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_INSUFFICIENT_BALANCE);
    }
}
