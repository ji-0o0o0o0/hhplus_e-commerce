package com.hhplus.hhplus_ecommerce.cart.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private LocalDateTime createdAt;

    public static CartItem create(Long userId, Long productId, Integer quantity) {
        return CartItem.builder()
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 비즈니스 로직: 수량 변경
    public void updateQuantity(Integer quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
        }
        this.quantity = quantity;
    }

    public void increaseQuantity(Integer amount) {
        this.quantity += amount;
    }

    public void decreaseQuantity(Integer amount) {
        this.quantity -= amount;
    }


    public boolean isQuantityZeroOrLess() {
        return this.quantity <= 0;
    }
}