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



    // 주문 생성
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

    // 비즈니스 로직: 총 금액 계산
    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .mapToLong(OrderItem::getSubtotal)
                .sum();
        this.finalAmount = this.totalAmount - this.discountAmount;
    }

    // 비즈니스 로직: 결제 가능 여부 체크
    public boolean canPay() {
        return this.status == OrderStatus.PENDING && this.finalAmount > 0;
    }

    // 비즈니스 로직: 주문 완료
    public void complete() {
        if (!canPay()) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_PAY);
        }
        this.status = OrderStatus.COMPLETED;
    }

    // 비즈니스 로직: 주문 취소
    public void cancel() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL);
        }
        this.status = OrderStatus.CANCELLED;
    }

    // 주문 항목 추가
    public void addItem(OrderItem item) {
        this.items.add(item);
        calculateTotalAmount();
    }

    // 할인 금액 적용
    public void applyDiscount(Long discountAmount) {
        this.discountAmount = discountAmount;
        calculateTotalAmount();
    }
}