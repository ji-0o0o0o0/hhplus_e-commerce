package com.hhplus.hhplus_ecommerce.cart.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

class CartItemTest {

    @Test
    @DisplayName("장바구니 항목을 생성할 수 있다")
    void create_성공() {
        // when
        CartItem cartItem = CartItem.create(1L, 2L, 3);

        // then
        assertAll(
                () -> assertThat(cartItem.getUserId()).isEqualTo(1L),
                () -> assertThat(cartItem.getProductId()).isEqualTo(2L),
                () -> assertThat(cartItem.getQuantity()).isEqualTo(3),
                () -> assertThat(cartItem.getCreatedAt()).isNotNull()
        );
    }

    @Test
    @DisplayName("수량을 변경할 수 있다")
    void updateQuantity_성공() {
        // given
        CartItem cartItem = CartItem.create(1L, 2L, 3);

        // when
        cartItem.updateQuantity(5);

        // then
        assertThat(cartItem.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("0 이하의 수량으로 변경할 수 없다")
    void updateQuantity_0이하_예외() {
        // given
        CartItem cartItem = CartItem.create(1L, 2L, 3);

        // when & then
        assertThatThrownBy(() -> cartItem.updateQuantity(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("수량은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("음수 수량으로 변경할 수 없다")
    void updateQuantity_음수_예외() {
        // given
        CartItem cartItem = CartItem.create(1L, 2L, 3);

        // when & then
        assertThatThrownBy(() -> cartItem.updateQuantity(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("수량은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("수량을 증가시킬 수 있다")
    void increaseQuantity_성공() {
        // given
        CartItem cartItem = CartItem.create(1L, 2L, 3);

        // when
        cartItem.increaseQuantity(2);

        // then
        assertThat(cartItem.getQuantity()).isEqualTo(5);  // 3 + 2
    }
}