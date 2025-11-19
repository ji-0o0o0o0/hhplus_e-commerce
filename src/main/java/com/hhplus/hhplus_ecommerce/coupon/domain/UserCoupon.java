package com.hhplus.hhplus_ecommerce.coupon.domain;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupons")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCoupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long couponId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer discountRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column
    private LocalDateTime usedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // 쿠폰 발급
    public static UserCoupon issue(Long userId, Coupon coupon) {
        return UserCoupon.builder()
                .userId(userId)
                .couponId(coupon.getId())
                .name(coupon.getName())
                .discountRate(coupon.getDiscountRate())
                .status(CouponStatus.AVAILABLE)
                .issuedAt(LocalDateTime.now())
                .usedAt(null)
                .expiresAt(LocalDateTime.now().plusDays(coupon.getValidityDays()))
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

    public boolean shouldExpire() {
        return this.status == CouponStatus.AVAILABLE &&
                this.expiresAt != null &&
                LocalDateTime.now().isAfter(this.expiresAt);
    }

    // 할인 금액 계산
    public Long calculateDiscount(Long orderAmount) {
        return orderAmount * discountRate / 100;
    }
}