package com.hhplus.hhplus_ecommerce.order.domain;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

class OrderTest {

    @Test
    @DisplayName("주문을 생성할 수 있다")
    void create_성공() {
        // given
        Product product = Product.create("노트북", "고성능", 2000000, 10, "전자제품");
        OrderItem item = OrderItem.create(product, 2);
        List<OrderItem> items = List.of(item);

        // when
        Order order = Order.create(1L, items, null, 0);

        // then
        assertAll(
                () -> assertThat(order.getTotalAmount()).isEqualTo(4000000),
                () -> assertThat(order.getFinalAmount()).isEqualTo(4000000),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING)
        );
    }

    @Test
    @DisplayName("쿠폰 할인이 적용된 주문을 생성할 수 있다")
    void create_쿠폰할인_성공() {
        // given
        Product product = Product.create("노트북", "고성능", 1000000, 10, "전자제품");
        OrderItem item = OrderItem.create(product, 1);
        List<OrderItem> items = List.of(item);
        Integer discountAmount = 100000; // 10% 할인

        // when
        Order order = Order.create(1L, items, 1L, discountAmount);

        // then
        assertAll(
                () -> assertThat(order.getTotalAmount()).isEqualTo(1000000),
                () -> assertThat(order.getDiscountAmount()).isEqualTo(100000),
                () -> assertThat(order.getFinalAmount()).isEqualTo(900000),
                () -> assertThat(order.getCouponId()).isEqualTo(1L)
        );
    }

    @Test
    @DisplayName("주문을 완료할 수 있다")
    void complete_성공() {
        // given
        Product product = Product.create("노트북", "고성능", 1000000, 10, "전자제품");
        OrderItem item = OrderItem.create(product, 1);
        Order order = Order.create(1L, List.of(item), null, 0);

        // when
        order.complete();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("결제할 수 없는 주문은 완료할 수 없다")
    void complete_결제불가_예외() {
        // given
        Product product = Product.create("노트북", "고성능", 1000000, 10, "전자제품");
        OrderItem item = OrderItem.create(product, 1);
        Order order = Order.create(1L, List.of(item), null, 0);
        order.complete(); // 이미 완료됨

        // when & then
        assertThatThrownBy(() -> order.complete())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANNOT_PAY);
    }

    @Test
    @DisplayName("주문을 취소할 수 있다")
    void cancel_성공() {
        // given
        Product product = Product.create("노트북", "고성능", 1000000, 10, "전자제품");
        OrderItem item = OrderItem.create(product, 1);
        Order order = Order.create(1L, List.of(item), null, 0);

        // when
        order.cancel();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("완료된 주문은 취소할 수 없다")
    void cancel_완료된주문_예외() {
        // given
        Product product = Product.create("노트북", "고성능", 1000000, 10, "전자제품");
        OrderItem item = OrderItem.create(product, 1);
        Order order = Order.create(1L, List.of(item), null, 0);
        order.complete();

        // when & then
        assertThatThrownBy(() -> order.cancel())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANNOT_CANCEL);
    }
}