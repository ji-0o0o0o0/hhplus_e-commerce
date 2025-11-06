package com.hhplus.hhplus_ecommerce.order.application;

import com.hhplus.hhplus_ecommerce.cart.domain.CartItem;
import com.hhplus.hhplus_ecommerce.cart.repository.CartItemRepository;
import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
import com.hhplus.hhplus_ecommerce.order.repository.OrderRepository;
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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private com.hhplus.hhplus_ecommerce.common.lock.LockManager lockManager;

    @InjectMocks
    private OrderService orderService;

    private Long userId;
    private Long productId;
    private Long cartItemId;
    private Product product;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        userId = 1L;
        productId = 1L;
        cartItemId = 1L;

        product = Product.create("노트북", "고성능", 1000000, 10, "전자제품");
        product = Product.builder()
                .id(productId)
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .category(product.getCategory())
                .build();

        cartItem = CartItem.builder()
                .id(cartItemId)
                .userId(userId)
                .productId(productId)
                .quantity(2)
                .build();

        // LockManager Mock 공통 설정 (lenient)
        lenient().doAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).when(lockManager).executeWithLock(anyString(), any(Runnable.class));
    }

    @Test
    @DisplayName("주문을 생성할 수 있다")
    void createOrder_성공() {
        // given
        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.of(cartItem));
        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // LockManager Mock: executeWithLock 호출 시 action 실행
        willAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).given(lockManager).executeWithLock(anyString(), any(Runnable.class));

        given(productRepository.save(any(Product.class))).willReturn(product);
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Order result = orderService.createOrder(userId, List.of(cartItemId), null);

        // then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getUserId()).isEqualTo(userId),
                () -> assertThat(result.getTotalAmount()).isEqualTo(2000000),
                () -> assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING)
        );
        verify(lockManager).executeWithLock(eq("product:" + productId), any(Runnable.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("쿠폰을 적용하여 주문할 수 있다")
    void createOrder_쿠폰적용() {
        // given
        Long couponId = 1L;
        UserCoupon userCoupon = UserCoupon.builder()
                .id(1L)
                .userId(userId)
                .couponId(couponId)
                .discountRate(10)
                .status(com.hhplus.hhplus_ecommerce.coupon.CouponStatus.AVAILABLE)
                .issuedAt(java.time.LocalDateTime.now())
                .expiresAt(java.time.LocalDateTime.now().plusDays(7))
                .build();

        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.of(cartItem));
        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // LockManager Mock
        willAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).given(lockManager).executeWithLock(anyString(), any(Runnable.class));

        given(productRepository.save(any(Product.class))).willReturn(product);
        given(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .willReturn(Optional.of(userCoupon));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Order result = orderService.createOrder(userId, List.of(cartItemId), couponId);

        // then
        assertAll(
                () -> assertThat(result.getTotalAmount()).isEqualTo(2000000),
                () -> assertThat(result.getDiscountAmount()).isGreaterThan(0),
                () -> assertThat(result.getFinalAmount()).isLessThan(2000000)
        );
    }

    @Test
    @DisplayName("장바구니 아이템이 없으면 주문할 수 없다")
    void createOrder_장바구니없음_예외() {
        // given
        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(userId, List.of(cartItemId), null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자의 장바구니로는 주문할 수 없다")
    void createOrder_권한없음_예외() {
        // given
        CartItem otherUserCart = CartItem.builder()
                .id(cartItemId)
                .userId(999L)  // 다른 사용자
                .productId(productId)
                .quantity(2)
                .build();

        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.of(otherUserCart));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(userId, List.of(cartItemId), null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_ACCESS_DENIED);
    }

    @Test
    @DisplayName("재고가 부족하면 주문할 수 없다")
    void createOrder_재고부족_예외() {
        // given
        Product lowStockProduct = Product.builder()
                .id(productId)
                .name("노트북")
                .price(1000000)
                .stock(1)  // 재고 부족
                .category("전자제품")
                .build();

        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.of(cartItem));
        given(productRepository.findById(productId)).willReturn(Optional.of(lowStockProduct));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(userId, List.of(cartItemId), null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("주문을 조회할 수 있다")
    void getOrder_성공() {
        // given
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .build();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // when
        Order result = orderService.getOrder(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("존재하지 않는 주문을 조회하면 예외가 발생한다")
    void getOrder_주문없음_예외() {
        // given
        Long orderId = 999L;
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.getOrder(orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자별 주문 목록을 조회할 수 있다")
    void getOrdersByUserId_성공() {
        // given
        Order order1 = Order.builder().id(1L).userId(userId).build();
        Order order2 = Order.builder().id(2L).userId(userId).build();
        given(orderRepository.findByUserId(userId)).willReturn(List.of(order1, order2));

        // when
        List<Order> result = orderService.getOrdersByUserId(userId);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("주문을 취소할 수 있다")
    void cancelOrder_성공() {
        // given
        Long orderId = 1L;
        Order order = Order.create(userId, List.of(), null, 0);
        order = Order.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .items(List.of())
                .build();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // when
        orderService.cancelOrder(orderId);

        // then
        verify(orderRepository).save(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}