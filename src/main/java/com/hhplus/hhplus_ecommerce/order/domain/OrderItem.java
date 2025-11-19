package com.hhplus.hhplus_ecommerce.order.domain;

import com.hhplus.hhplus_ecommerce.product.domain.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private Long unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Long subtotal;

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
                                   String productName, Long unitPrice, Integer quantity) {
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

    public Long calculateSubtotal() {
        return this.unitPrice * this.quantity;
    }
}