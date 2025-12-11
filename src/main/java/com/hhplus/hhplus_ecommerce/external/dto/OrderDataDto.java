package com.hhplus.hhplus_ecommerce.external.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@AllArgsConstructor
public class OrderDataDto {
    private Long orderId;
    private Long userId;
    private Long totalAmount;
    private Long discountAmount;
    private Long finalAmount;
    private CouponData coupon;     
    private String status;
    private List<OrderItemData> items;
    private LocalDateTime completedAt;

    @Getter
    @AllArgsConstructor
    public static class OrderItemData {
        private Long productId;
        private String productName;
        private Integer quantity;
        private Long unitPrice;
    }

    @Getter
    @AllArgsConstructor
    public static class CouponData {
        private Long couponId;
        private String couponName;
    }
}