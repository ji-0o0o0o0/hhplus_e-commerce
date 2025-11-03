package com.hhplus.hhplus_ecommerce.coupon.domain;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCoupon {
    private Long id;
    private Long userId;
    private Long couponId;
    private String name;
    private Integer discountRate;
    private CouponStatus status;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
    private LocalDateTime expiresAt;

    // 쿠폰 발급
    public static UserCoupon issue(Long userId, Coupon coupon, LocalDateTime expiresAt) {
        return UserCoupon.builder()
                .userId(userId)
                .couponId(coupon.getId())
                .name(coupon.getName())
                .discountRate(coupon.getDiscountRate())
                .status(CouponStatus.AVAILABLE)
                .issuedAt(LocalDateTime.now())
                .usedAt(null)
                .expiresAt(expiresAt)
                .build();
    }

    // 비즈니스 로직: 사용 가능 여부
    public boolean isAvailable() {
        if (this.status != CouponStatus.AVAILABLE) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isAfter(this.expiresAt);
    }

    // 비즈니스 로직: 쿠폰 사용
    public void use() {
        if (!isAvailable()) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    // 비즈니스 로직: 쿠폰 만료 처리
    public void expire() {
        this.status = CouponStatus.EXPIRED;
    }

    // 할인 금액 계산
    public int calculateDiscount(Integer orderAmount) {
        return orderAmount * discountRate / 100;
    }
}