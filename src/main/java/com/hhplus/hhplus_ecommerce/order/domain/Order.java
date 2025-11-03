package com.hhplus.hhplus_ecommerce.order.domain;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long id;
    private Long userId;
    private Long couponId;
    private Integer totalAmount;
    private Integer discountAmount;
    private Integer finalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 주문 생성
    public static Order create( Long userId, Long couponId,
                               Integer totalAmount, Integer discountAmount) {

        return Order.builder()
                .userId(userId)
                .couponId(couponId)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(totalAmount - discountAmount)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // 비즈니스 로직: 주문 완료
    public void complete() {
        this.status = OrderStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 로직: 주문 취소
    public void cancel() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL);
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }
}