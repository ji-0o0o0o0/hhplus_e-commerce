package com.hhplus.hhplus_ecommerce.coupon.domain;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

class UserCouponTest {

    @Test
    @DisplayName("사용자 쿠폰을 발급할 수 있다")
    void issue_성공() {
        // given
        Coupon coupon = Coupon.create(
                "10% 할인",
                10,
                100,
                7,  // 7일간 유효
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );
        coupon = Coupon.builder()
                .id(1L)
                .name(coupon.getName())
                .discountRate(coupon.getDiscountRate())
                .totalQuantity(coupon.getTotalQuantity())
                .validityDays(7)
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .build();

        // when
        UserCoupon userCoupon = UserCoupon.issue(1L, coupon);

        // then
        assertAll(
                () -> assertThat(userCoupon.getUserId()).isEqualTo(1L),
                () -> assertThat(userCoupon.getCouponId()).isEqualTo(1L),
                () -> assertThat(userCoupon.getName()).isEqualTo("10% 할인"),
                () -> assertThat(userCoupon.getDiscountRate()).isEqualTo(10),
                () -> assertThat(userCoupon.getStatus()).isEqualTo(CouponStatus.AVAILABLE)
        );
    }

    @Test
    @DisplayName("사용 가능한 쿠폰인지 확인할 수 있다")
    void isAvailable_사용가능() {
        // given
        Coupon coupon = createCouponWithId(1L, 7);
        UserCoupon userCoupon = UserCoupon.issue(1L, coupon);

        // when & then
        assertThat(userCoupon.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("만료된 쿠폰은 사용할 수 없다")
    void isAvailable_만료됨() {
        // given
        Coupon coupon = createCouponWithId(1L, -1);  // 이미 만료
        UserCoupon userCoupon = UserCoupon.issue(1L, coupon);

        // when & then
        assertThat(userCoupon.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("이미 사용한 쿠폰은 사용할 수 없다")
    void isAvailable_이미사용() {
        // given
        Coupon coupon = createCouponWithId(1L, 7);
        UserCoupon userCoupon = UserCoupon.issue(1L, coupon);
        userCoupon.use();

        // when & then
        assertThat(userCoupon.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("쿠폰을 사용할 수 있다")
    void use_성공() {
        // given
        Coupon coupon = createCouponWithId(1L, 7);
        UserCoupon userCoupon = UserCoupon.issue(1L, coupon);

        // when
        userCoupon.use();

        // then
        assertAll(
                () -> assertThat(userCoupon.getStatus()).isEqualTo(CouponStatus.USED),
                () -> assertThat(userCoupon.getUsedAt()).isNotNull()
        );
    }

    @Test
    @DisplayName("사용 불가능한 쿠폰은 사용할 수 없다")
    void use_사용불가_예외() {
        // given
        Coupon coupon = createCouponWithId(1L, 7);
        UserCoupon userCoupon = UserCoupon.issue(1L, coupon);
        userCoupon.use();  // 이미 사용

        // when & then
        assertThatThrownBy(() -> userCoupon.use())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("쿠폰을 만료 처리할 수 있다")
    void expire_성공() {
        // given
        Coupon coupon = createCouponWithId(1L, 7);
        UserCoupon userCoupon = UserCoupon.issue(1L, coupon);

        // when
        userCoupon.expire();

        // then
        assertThat(userCoupon.getStatus()).isEqualTo(CouponStatus.EXPIRED);
    }

    @Test
    @DisplayName("할인 금액을 계산할 수 있다")
    void calculateDiscount_성공() {
        // given
        Coupon coupon = createCouponWithId(1L, 7);
        UserCoupon userCoupon = UserCoupon.issue(1L, coupon);

        // when
        Long discount = userCoupon.calculateDiscount(100000L);

        // then
        assertThat(discount).isEqualTo(10000);  // 10% 할인
    }

    // Helper method
    private Coupon createCouponWithId(Long id, int validityDays) {
        return Coupon.builder()
                .id(id)
                .name("10% 할인")
                .discountRate(10)
                .totalQuantity(100)
                .validityDays(validityDays)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .build();
    }
}