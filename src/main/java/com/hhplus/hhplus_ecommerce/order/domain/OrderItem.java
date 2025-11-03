package com.hhplus.hhplus_ecommerce.order.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private Integer unitPrice;
    private Integer quantity;
    private Integer subtotal;

    // 주문 항목 생성
    public static OrderItem create( Long orderId, Long productId,
                                   String productName, Integer unitPrice, Integer quantity) {
        return OrderItem.builder()
                .orderId(orderId)
                .productId(productId)
                .productName(productName)
                .unitPrice(unitPrice)
                .quantity(quantity)
                .subtotal(unitPrice * quantity)
                .build();
    }

    // 소계 계산
    public Integer calculateSubtotal() {
        return this.unitPrice * this.quantity;
    }
}