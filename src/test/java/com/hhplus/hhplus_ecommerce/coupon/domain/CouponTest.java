package com.hhplus.hhplus_ecommerce.coupon.domain;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

class CouponTest {

    @Test
    @DisplayName("쿠폰 발급 수량을 증가시킬 수 있다")
    void increaseIssuedQuantity_성공() {
        // given
        Coupon coupon = Coupon.create(
                "10% 할인",
                10,
                100,
                7,  // validityDays
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        // when
        coupon.increaseIssuedQuantity();

        // then
        assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
        assertThat(coupon.getRemainingQuantity()).isEqualTo(99);
    }

    @Test
    @DisplayName("쿠폰이 모두 소진되면 발급할 수 없다")
    void increaseIssuedQuantity_수량소진_예외() {
        // given
        Coupon coupon = Coupon.create(
                "10% 할인",
                10,
                2,
                7,  
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );
        coupon.increaseIssuedQuantity();
        coupon.increaseIssuedQuantity();

        // when & then
        assertThatThrownBy(() -> coupon.increaseIssuedQuantity())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_SOLD_OUT);
    }

    @Test
    @DisplayName("쿠폰 할인 금액을 계산할 수 있다")
    void calculateDiscount_성공() {
        // given
        Coupon coupon = Coupon.create(
                "10% 할인",
                10,
                100,
                7,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        // when
        int discount = coupon.calculateDiscount(100000);

        // then
        assertThat(discount).isEqualTo(10000);
    }

    @Test
    @DisplayName("쿠폰 유효기간을 확인할 수 있다")
    void isValid_성공() {
        // given
        Coupon validCoupon = Coupon.create(
                "10% 할인",
                10,
                100,
                7,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );

        Coupon expiredCoupon = Coupon.create(
                "만료된 쿠폰",
                10,
                100,
                1,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(1)
        );

        // when & then
        assertAll(
                () -> assertThat(validCoupon.isValid()).isTrue(),
                () -> assertThat(expiredCoupon.isValid()).isFalse()
        );
    }
}