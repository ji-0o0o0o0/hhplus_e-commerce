package com.hhplus.hhplus_ecommerce.coupon.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon validCoupon;
    private Long userId;
    private Long couponId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        couponId = 1L;

        validCoupon = Coupon.builder()
                .id(couponId)
                .name("10% 할인")
                .discountRate(10)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validityDays(7)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Test
    @DisplayName("쿠폰을 발급할 수 있다")
    void issueCoupon_성공() {
        // given
        given(couponRepository.findById(couponId)).willReturn(Optional.of(validCoupon));
        given(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .willReturn(Optional.empty());
        given(userCouponRepository.save(any(UserCoupon.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        UserCoupon result = couponService.issueCoupon(userId, couponId);

        // then
        assertAll(
                () -> assertThat(result.getUserId()).isEqualTo(userId),
                () -> assertThat(result.getCouponId()).isEqualTo(couponId),
                () -> assertThat(result.getStatus()).isEqualTo(CouponStatus.AVAILABLE)
        );
        verify(couponRepository).save(validCoupon);
        verify(userCouponRepository).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰은 발급할 수 없다")
    void issueCoupon_쿠폰없음_예외() {
        // given
        given(couponRepository.findById(couponId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 발급받은 쿠폰은 다시 발급받을 수 없다")
    void issueCoupon_중복발급_예외() {
        // given
        UserCoupon existingUserCoupon = UserCoupon.issue(userId, validCoupon);
        given(couponRepository.findById(couponId)).willReturn(Optional.of(validCoupon));
        given(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .willReturn(Optional.of(existingUserCoupon));

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_ALREADY_ISSUED);
    }

    @Test
    @DisplayName("수량이 모두 소진된 쿠폰은 발급할 수 없다")
    void issueCoupon_수량소진_예외() {
        // given
        Coupon soldOutCoupon = Coupon.builder()
                .id(couponId)
                .name("10% 할인")
                .discountRate(10)
                .totalQuantity(100)
                .issuedQuantity(100)  // 모두 발급됨
                .validityDays(7)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .build();

        given(couponRepository.findById(couponId)).willReturn(Optional.of(soldOutCoupon));
        given(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_SOLD_OUT);
    }

    @Test
    @DisplayName("유효기간이 지난 쿠폰은 발급할 수 없다")
    void issueCoupon_유효기간지남_예외() {
        // given
        Coupon expiredCoupon = Coupon.builder()
                .id(couponId)
                .name("10% 할인")
                .discountRate(10)
                .totalQuantity(100)
                .issuedQuantity(0)
                .validityDays(7)
                .startDate(LocalDateTime.now().minusDays(30))
                .endDate(LocalDateTime.now().minusDays(1))  // 만료됨
                .build();

        given(couponRepository.findById(couponId)).willReturn(Optional.of(expiredCoupon));
        given(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("사용자의 쿠폰 목록을 조회할 수 있다")
    void getUserCoupons_성공() {
        // given
        UserCoupon userCoupon1 = UserCoupon.issue(userId, validCoupon);
        UserCoupon userCoupon2 = UserCoupon.issue(userId, validCoupon);
        given(userCouponRepository.findByUserId(userId))
                .willReturn(List.of(userCoupon1, userCoupon2));

        // when
        List<UserCoupon> result = couponService.getUserCoupons(userId);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("만료된 쿠폰은 자동으로 상태가 변경된다")
    void getUserCoupons_만료처리() {
        // given
        Coupon expiredCoupon = Coupon.builder()
                .id(couponId)
                .name("10% 할인")
                .discountRate(10)
                .totalQuantity(100)
                .validityDays(-1)  // 이미 만료
                .startDate(LocalDateTime.now().minusDays(30))
                .endDate(LocalDateTime.now().minusDays(1))
                .build();

        UserCoupon userCoupon = UserCoupon.issue(userId, expiredCoupon);
        given(userCouponRepository.findByUserId(userId))
                .willReturn(List.of(userCoupon));

        // when
        couponService.getUserCoupons(userId);

        // then
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
        assertThat(userCoupon.getStatus()).isEqualTo(CouponStatus.EXPIRED);
    }

    @Test
    @DisplayName("사용 가능한 쿠폰만 조회할 수 있다")
    void getAvailableUserCoupons_성공() {
        // given
        UserCoupon userCoupon = UserCoupon.issue(userId, validCoupon);
        given(userCouponRepository.findByUserIdAndStatus(userId, CouponStatus.AVAILABLE))
                .willReturn(List.of(userCoupon));

        // when
        List<UserCoupon> result = couponService.getAvailableUserCoupons(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(CouponStatus.AVAILABLE);
    }

    @Test
    @DisplayName("발급 가능한 쿠폰 목록을 조회할 수 있다")
    void getAvailableCoupons_성공() {
        // given
        given(couponRepository.findAvailableCoupons())
                .willReturn(List.of(validCoupon));

        // when
        List<Coupon> result = couponService.getAvailableCoupons();

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("쿠폰을 조회할 수 있다")
    void getCoupon_성공() {
        // given
        given(couponRepository.findById(couponId)).willReturn(Optional.of(validCoupon));

        // when
        Coupon result = couponService.getCoupon(couponId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(couponId);
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰을 조회하면 예외가 발생한다")
    void getCoupon_쿠폰없음_예외() {
        // given
        given(couponRepository.findById(couponId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.getCoupon(couponId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_FOUND);
    }
}