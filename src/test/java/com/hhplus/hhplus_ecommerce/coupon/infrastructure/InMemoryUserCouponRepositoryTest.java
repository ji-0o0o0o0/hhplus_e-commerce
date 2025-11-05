package com.hhplus.hhplus_ecommerce.coupon.infrastructure;

import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class InMemoryUserCouponRepositoryTest {

    private InMemoryUserCouponRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserCouponRepository();
    }

    @Test
    @DisplayName("새로운 사용자 쿠폰을 저장할 수 있다")
    void save_신규_성공() {
        // given
        Coupon coupon = Coupon.create("10% 할인", 10, 100, 7,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30));
        UserCoupon userCoupon = UserCoupon.issue(1L, coupon);

        // when
        UserCoupon saved = repository.save(userCoupon);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getStatus()).isEqualTo(CouponStatus.AVAILABLE);
    }

    @Test
    @DisplayName("기존 사용자 쿠폰을 업데이트할 수 있다")
    void save_업데이트_성공() {
        // given
        Coupon coupon = Coupon.create("10% 할인", 10, 100, 7,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30));
        UserCoupon userCoupon = UserCoupon.issue(1L, coupon);
        UserCoupon saved = repository.save(userCoupon);

        // when
        UserCoupon updated = UserCoupon.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .couponId(saved.getCouponId())
                .name(saved.getName())
                .discountRate(saved.getDiscountRate())
                .status(CouponStatus.USED)
                .issuedAt(saved.getIssuedAt())
                .usedAt(LocalDateTime.now())
                .expiresAt(saved.getExpiresAt())
                .build();
        UserCoupon result = repository.save(updated);

        // then
        assertThat(result.getStatus()).isEqualTo(CouponStatus.USED);
    }

    @Test
    @DisplayName("ID로 사용자 쿠폰을 조회할 수 있다")
    void findById_성공() {
        // given
        Coupon coupon = Coupon.create("10% 할인", 10, 100, 7,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30));
        UserCoupon userCoupon = UserCoupon.issue(1L, coupon);
        UserCoupon saved = repository.save(userCoupon);

        // when
        Optional<UserCoupon> found = repository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 쿠폰은 조회되지 않는다")
    void findById_없음() {
        // when
        Optional<UserCoupon> found = repository.findById(999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID로 쿠폰 목록을 조회할 수 있다")
    void findByUserId_성공() {
        // given
        Coupon coupon1 = Coupon.create("10% 할인", 10, 100, 7,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30));
        Coupon coupon2 = Coupon.create("20% 할인", 20, 50, 7,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30));

        repository.save(UserCoupon.issue(1L, coupon1));
        repository.save(UserCoupon.issue(1L, coupon2));
        repository.save(UserCoupon.issue(2L, coupon1));

        // when
        List<UserCoupon> userCoupons = repository.findByUserId(1L);

        // then
        assertThat(userCoupons).hasSize(2);
    }

    @Test
    @DisplayName("사용자 ID와 쿠폰 ID로 사용자 쿠폰을 조회할 수 있다")
    void findByUserIdAndCouponId_성공() {
        // given
        Coupon coupon = Coupon.builder()
                .id(10L)
                .name("10% 할인")
                .discountRate(10)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validityDays(7)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .build();

        repository.save(UserCoupon.issue(1L, coupon));

        // when
        Optional<UserCoupon> found = repository.findByUserIdAndCouponId(1L, 10L);

        // then
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("사용자 ID와 상태로 쿠폰 목록을 조회할 수 있다")
    void findByUserIdAndStatus_성공() {
        // given
        Coupon coupon = Coupon.create("10% 할인", 10, 100, 7,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30));
        UserCoupon userCoupon1 = UserCoupon.issue(1L, coupon);
        UserCoupon saved1 = repository.save(userCoupon1);

        UserCoupon userCoupon2 = UserCoupon.issue(1L, coupon);
        UserCoupon saved2 = repository.save(userCoupon2);

        // 하나는 사용 처리
        UserCoupon used = UserCoupon.builder()
                .id(saved1.getId())
                .userId(saved1.getUserId())
                .couponId(saved1.getCouponId())
                .name(saved1.getName())
                .discountRate(saved1.getDiscountRate())
                .status(CouponStatus.USED)
                .issuedAt(saved1.getIssuedAt())
                .usedAt(LocalDateTime.now())
                .expiresAt(saved1.getExpiresAt())
                .build();
        repository.save(used);

        // when
        List<UserCoupon> availableCoupons = repository.findByUserIdAndStatus(1L, CouponStatus.AVAILABLE);
        List<UserCoupon> usedCoupons = repository.findByUserIdAndStatus(1L, CouponStatus.USED);

        // then
        assertThat(availableCoupons).hasSize(1);
        assertThat(usedCoupons).hasSize(1);
    }

    @Test
    @DisplayName("저장소를 초기화할 수 있다")
    void clear_성공() {
        // given
        Coupon coupon = Coupon.create("10% 할인", 10, 100, 7,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30));
        repository.save(UserCoupon.issue(1L, coupon));
        repository.save(UserCoupon.issue(2L, coupon));

        // when
        repository.clear();

        // then
        assertThat(repository.findByUserId(1L)).isEmpty();
        assertThat(repository.findByUserId(2L)).isEmpty();
    }
}