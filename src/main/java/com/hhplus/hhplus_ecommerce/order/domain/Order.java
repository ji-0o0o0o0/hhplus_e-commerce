package com.hhplus.hhplus_ecommerce.order.domain;

import com.hhplus.hhplus_ecommerce.common.BaseTimeEntity;
import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "orders")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column
    private Long couponId;

    @Transient
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private Long totalAmount;

    @Column(nullable = false)
    private Long discountAmount;

    @Column(nullable = false)
    private Long finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private OrderStatus status;



    public static Order create(Long userId, List<OrderItem> items, Long couponId, Long discountAmount) {
        Order order = Order.builder()
                .userId(userId)
                .items(items != null ? new ArrayList<>(items) : new ArrayList<>())
                .couponId(couponId)
                .discountAmount(discountAmount != null ? discountAmount : 0)
                .status(OrderStatus.PENDING)
                .build();

        order.calculateTotalAmount();
        return order;
    }

    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .mapToLong(OrderItem::getSubtotal)
                .sum();
        this.finalAmount = this.totalAmount - this.discountAmount;
    }

    public boolean canPay() {
        return this.status == OrderStatus.PENDING && this.finalAmount > 0;
    }

    public void complete() {
        if (!canPay()) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_PAY);
        }
        this.status = OrderStatus.COMPLETED;
    }

    public void cancel() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL);
        }
        this.status = OrderStatus.CANCELLED;
    }

    public void addItem(OrderItem item) {
        this.items.add(item);
        calculateTotalAmount();
    }

    public void applyDiscount(Long discountAmount) {
        this.discountAmount = discountAmount;
        calculateTotalAmount();
    }
}