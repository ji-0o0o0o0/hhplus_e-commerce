package com.hhplus.hhplus_ecommerce.common.event;

import java.time.LocalDateTime;
import java.util.List;


public record PaymentCompletedEvent(
        Long orderId,
        Long userId,
        Long totalAmount,
        Long discountAmount,
        Long finalAmount,
        Long couponId,
        List<OrderItemInfo> items,
        LocalDateTime completedAt
) {
    public record OrderItemInfo(
            Long productId,
            String productName,
            Integer quantity,
            Long unitPrice
    ) {}
}