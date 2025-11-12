package com.hhplus.hhplus_ecommerce.order.domain;

import com.hhplus.hhplus_ecommerce.product.domain.Product;
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

    // 주문 항목 생성 (Product 객체로부터)
    public static OrderItem create(Product product, Integer quantity) {
        OrderItem item = OrderItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(quantity)
                .build();
        item.subtotal = item.calculateSubtotal();
        return item;
    }

    // 주문 항목 생성 (개별 필드로부터)
    public static OrderItem create(Long orderId, Long productId,
                                   String productName, Integer unitPrice, Integer quantity) {
        OrderItem item = OrderItem.builder()
                .orderId(orderId)
                .productId(productId)
                .productName(productName)
                .unitPrice(unitPrice)
                .quantity(quantity)
                .build();
        item.subtotal = item.calculateSubtotal();
        return item;
    }

    public Integer calculateSubtotal() {
        return this.unitPrice * this.quantity;
    }
}