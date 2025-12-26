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
@Table(
    name = "user_coupons",
    indexes = {
        @Index(name = "idx_user_coupon", columnList = "user_id, coupon_id", unique = true),
        @Index(name = "idx_request_id", columnList = "request_id", unique = true),
        @Index(name = "idx_user_status", columnList = "user_id, status")
    }
)
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

    @Column(length = 100)
    private String requestId;  // Kafka 비동기 발급 시 요청 ID (멱등성 키)

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

    public static UserCoupon issue(Long userId, Coupon coupon) {
        return issue(userId, coupon, null);
    }

    public static UserCoupon issue(Long userId, Coupon coupon, String requestId) {
        return UserCoupon.builder()
                .userId(userId)
                .couponId(coupon.getId())
                .requestId(requestId)
                .name(coupon.getName())
                .discountRate(coupon.getDiscountRate())
                .status(CouponStatus.AVAILABLE)
                .issuedAt(LocalDateTime.now())
                .usedAt(null)
                .expiresAt(LocalDateTime.now().plusDays(coupon.getValidityDays()))
                .build();
    }

    public boolean isAvailable() {
        if (this.status != CouponStatus.AVAILABLE) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isAfter(this.expiresAt);
    }

    public void use() {
        if (!isAvailable()) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    public void rollback() {
        if (this.status == CouponStatus.USED) {
            this.status = CouponStatus.AVAILABLE;
            this.usedAt = null;
        }
    }

    public void expire() {
        this.status = CouponStatus.EXPIRED;
    }

    public boolean shouldExpire() {
        return this.status == CouponStatus.AVAILABLE &&
                this.expiresAt != null &&
                LocalDateTime.now().isAfter(this.expiresAt);
    }

    public Long calculateDiscount(Long orderAmount) {
        return orderAmount * discountRate / 100;
    }
}