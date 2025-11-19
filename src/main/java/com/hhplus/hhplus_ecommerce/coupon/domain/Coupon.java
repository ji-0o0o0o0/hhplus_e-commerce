package com.hhplus.hhplus_ecommerce.coupon.domain;

import com.hhplus.hhplus_ecommerce.common.BaseTimeEntity;
import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,length = 100)
    private String name;

    @Column(nullable = false)
    private Integer discountRate;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer issuedQuantity;

    @Column(nullable = false)
    private Integer validityDays;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Version
    private Long version;

    public static Coupon create( String name, Integer discountRate,
                                Integer totalQuantity,Integer validityDays, LocalDateTime startDate, LocalDateTime endDate) {
        return Coupon.builder()
                .name(name)
                .discountRate(discountRate)
                .totalQuantity(totalQuantity)
                .issuedQuantity(0)  // 초기 발급 수량 0
                .validityDays(validityDays)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    public boolean canIssue() {
        return this.issuedQuantity < this.totalQuantity;
    }

    public void increaseIssuedQuantity() {
        if (!canIssue()) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }
        this.issuedQuantity++;
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    public int getRemainingQuantity() {
        return this.totalQuantity - this.issuedQuantity;
    }

    public int calculateDiscount(Integer orderAmount) {
        return orderAmount * discountRate / 100;
    }
}
