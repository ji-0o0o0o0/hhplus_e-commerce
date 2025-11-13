package com.hhplus.hhplus_ecommerce.payment.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
import com.hhplus.hhplus_ecommerce.order.domain.OrderItem;
import com.hhplus.hhplus_ecommerce.order.repository.OrderRepository;
import com.hhplus.hhplus_ecommerce.point.domain.Point;
import com.hhplus.hhplus_ecommerce.point.domain.PointTransaction;
import com.hhplus.hhplus_ecommerce.point.repository.PointRepository;
import com.hhplus.hhplus_ecommerce.point.repository.PointTransactionRepository;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Long userId;
    private Long orderId;
    private Order order;
    private Point point;
    private Product product;

    @BeforeEach
    void setUp() {
        userId = 1L;
        orderId = 1L;

        product = Product.builder()
                .id(1L)
                .name("노트북")
                .price(1000000L)
                .stock(10)
                .category("전자제품")
                .build();

        OrderItem orderItem = OrderItem.builder()
                .productId(1L)
                .productName("노트북")
                .quantity(1)
                .unitPrice(1000000L)
                .subtotal(1000000L)
                .build();

        order = Order.builder()
                .id(orderId)
                .userId(userId)
                .items(List.of(orderItem))
                .totalAmount(1000000L)
                .discountAmount(0L)
                .finalAmount(1000000L)
                .status(OrderStatus.PENDING)
                .build();

        point = Point.builder()
                .id(1L)
                .userId(userId)
                .amount(1500000L)
                .build();
    }

    @Test
    @DisplayName("결제를 성공적으로 처리할 수 있다")
    void executePayment_성공() {
        // given
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(pointRepository.save(any(Point.class))).willAnswer(inv -> inv.getArgument(0));
        given(pointTransactionRepository.save(any(PointTransaction.class))).willAnswer(inv -> inv.getArgument(0));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        paymentService.executePayment(userId, orderId);

        // then
        verify(pointRepository).save(point);
        verify(pointTransactionRepository).save(any(PointTransaction.class));
        verify(orderRepository).save(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(point.getAmount()).isEqualTo(500000);  // 1500000 - 1000000
    }

    @Test
    @DisplayName("쿠폰이 있는 경우 쿠폰을 사용 처리한다")
    void executePayment_쿠폰사용() {
        // given
        Long couponId = 1L;
        Order orderWithCoupon = Order.builder()
                .id(orderId)
                .userId(userId)
                .items(order.getItems())
                .totalAmount(1000000L)
                .discountAmount(100000L)
                .finalAmount(900000L)
                .couponId(couponId)
                .status(OrderStatus.PENDING)
                .build();

        UserCoupon userCoupon = UserCoupon.builder()
                .id(1L)
                .userId(userId)
                .couponId(couponId)
                .status(com.hhplus.hhplus_ecommerce.coupon.CouponStatus.AVAILABLE)
                .issuedAt(java.time.LocalDateTime.now())
                .expiresAt(java.time.LocalDateTime.now().plusDays(7))
                .build();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(orderWithCoupon));
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .willReturn(Optional.of(userCoupon));
        given(pointRepository.save(any(Point.class))).willAnswer(inv -> inv.getArgument(0));
        given(pointTransactionRepository.save(any(PointTransaction.class))).willAnswer(inv -> inv.getArgument(0));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));
        given(userCouponRepository.save(any(UserCoupon.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        paymentService.executePayment(userId, orderId);

        // then
        verify(userCouponRepository).save(userCoupon);
    }

    @Test
    @DisplayName("존재하지 않는 주문은 결제할 수 없다")
    void executePayment_주문없음_예외() {
        // given
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.executePayment(userId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자의 주문은 결제할 수 없다")
    void executePayment_권한없음_예외() {
        // given
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> paymentService.executePayment(999L, orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 완료된 주문은 결제할 수 없다")
    void executePayment_이미완료_예외() {
        // given
        Order completedOrder = Order.builder()
                .id(orderId)
                .userId(userId)
                .items(order.getItems())
                .totalAmount(1000000L)
                .finalAmount(1000000L)
                .status(OrderStatus.COMPLETED)  // 이미 완료됨
                .build();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(completedOrder));

        // when & then
        assertThatThrownBy(() -> paymentService.executePayment(userId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANNOT_PAY);
    }

    @Test
    @DisplayName("포인트가 부족하면 결제할 수 없다")
    void executePayment_포인트부족_예외() {
        // given
        Point insufficientPoint = Point.builder()
                .id(1L)
                .userId(userId)
                .amount(500000L)  // 부족한 금액
                .build();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(insufficientPoint));

        // when & then
        assertThatThrownBy(() -> paymentService.executePayment(userId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("재고가 부족하면 결제할 수 없다")
    void executePayment_재고부족_예외() {
        // given
        Product lowStockProduct = Product.builder()
                .id(1L)
                .name("노트북")
                .price(1000000L)
                .stock(0)  // 재고 없음
                .category("전자제품")
                .build();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));
        given(productRepository.findById(1L)).willReturn(Optional.of(lowStockProduct));

        // when & then
        assertThatThrownBy(() -> paymentService.executePayment(userId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
    }
}