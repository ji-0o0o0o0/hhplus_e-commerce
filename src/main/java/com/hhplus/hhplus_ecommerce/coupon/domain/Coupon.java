package com.hhplus.hhplus_ecommerce.coupon.domain;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    private Long id;
    private String name;
    private Integer discountRate;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private Integer validityDays;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 새 쿠폰 생성
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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // 비즈니스 로직: 발급 가능 여부
    public boolean canIssue() {
        return this.issuedQuantity < this.totalQuantity;
    }

    // 비즈니스 로직: 발급 수량 증가
    public void increaseIssuedQuantity() {
        if (!canIssue()) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }
        this.issuedQuantity++;
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 로직: 유효기간 체크
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    // 남은 수량 조회
    public int getRemainingQuantity() {
        return this.totalQuantity - this.issuedQuantity;
    }

    // 할인 금액 계산
    public int calculateDiscount(Integer orderAmount) {
        return orderAmount * discountRate / 100;
    }
}
