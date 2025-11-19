package com.hhplus.hhplus_ecommerce.point.domain;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

class PointTest {

    @Test
    @DisplayName("포인트를 생성할 수 있다")
    void create_성공() {
        // when
        Point point = Point.create(1L);

        // then
        assertAll(
                () -> assertThat(point.getUserId()).isEqualTo(1L),
                () -> assertThat(point.getAmount()).isEqualTo(0)
        );
    }

    @Test
    @DisplayName("포인트를 충전할 수 있다")
    void charge_성공() {
        // given
        Point point = Point.create(1L);

        // when
        point.charge(10000L);

        // then
        assertThat(point.getAmount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("0원 이하는 충전할 수 없다")
    void charge_0원이하_예외() {
        // given
        Point point = Point.create(1L);

        // when & then
        assertThatThrownBy(() -> point.charge(0L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_INVALID_CHARGE_AMOUNT);
    }

    @Test
    @DisplayName("음수는 충전할 수 없다")
    void charge_음수_예외() {
        // given
        Point point = Point.create(1L);

        // when & then
        assertThatThrownBy(() -> point.charge(-1000L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_INVALID_CHARGE_AMOUNT);
    }

    @Test
    @DisplayName("1회 최대 충전 금액(100만원)을 초과할 수 없다")
    void charge_1회최대금액초과_예외() {
        // given
        Point point = Point.create(1L);

        // when & then
        assertThatThrownBy(() -> point.charge(1_000_001L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_CHARGE_AMOUNT_EXCEEDS_ONCE);
    }

    @Test
    @DisplayName("최대 보유 포인트(1000만원)를 초과할 수 없다")
    void charge_최대보유포인트초과_예외() {
        // given
        Point point = Point.create(1L);
        point.charge(1_000_000L);  // 1회 최대 금액으로 충전
        point.charge(1_000_000L);
        point.charge(1_000_000L);
        point.charge(1_000_000L);
        point.charge(1_000_000L);
        point.charge(1_000_000L);
        point.charge(1_000_000L);
        point.charge(1_000_000L);
        point.charge(1_000_000L);
        point.charge(1_000_000L);  // 총 10,000,000 (최대치)

        // when & then
        assertThatThrownBy(() -> point.charge(1L))  // 10,000,001이 되어 초과
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_MAX_BALANCE_EXCEEDED);
    }

    @Test
    @DisplayName("포인트를 사용할 수 있다")
    void use_성공() {
        // given
        Point point = Point.create(1L);
        point.charge(10000L);

        // when
        point.use(3000L);

        // then
        assertThat(point.getAmount()).isEqualTo(7000);
    }

    @Test
    @DisplayName("0원 이하는 사용할 수 없다")
    void use_0원이하_예외() {
        // given
        Point point = Point.create(1L);
        point.charge(10000L);

        // when & then
        assertThatThrownBy(() -> point.use(0L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_INVALID_USE_AMOUNT);
    }

    @Test
    @DisplayName("잔액보다 많은 금액은 사용할 수 없다")
    void use_잔액부족_예외() {
        // given
        Point point = Point.create(1L);
        point.charge(5000L);

        // when & then
        assertThatThrownBy(() -> point.use(10000L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_INSUFFICIENT_BALANCE);
    }
}