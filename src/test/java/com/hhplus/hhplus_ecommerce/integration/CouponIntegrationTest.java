package com.hhplus.hhplus_ecommerce.integration;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.application.CouponService;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import com.hhplus.hhplus_ecommerce.user.domain.User;
import com.hhplus.hhplus_ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

public class CouponIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private CouponService couponService;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private UserCouponRepository userCouponRepository;
    @Autowired
    private UserRepository userRepository;

    private User user;
    private Coupon coupon;

    @BeforeEach
    void setup(){
        user = userRepository.save(User.create("테스트 유저"));
        coupon = couponService.createCoupon("20% 할인쿠폰",20,10,30, LocalDateTime.now().minusDays(1),LocalDateTime.now().plusDays(30));
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void coupon_발급_성공() {
        //when
        UserCoupon userCoupon = couponService.issueCoupon(user.getId(), coupon.getId());

        //then
        assertAll(
                () -> assertThat(userCoupon.getUserId()).isEqualTo(user.getId()),
                () -> assertThat(userCoupon.getCouponId()).isEqualTo(coupon.getId()),
                () -> assertThat(userCoupon.getStatus()).isEqualTo(CouponStatus.AVAILABLE),
                () -> assertThat(userCoupon.getDiscountRate()).isEqualTo(20)
        );

        // 쿠폰 발급 수량 증가 확인
        Coupon updatedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("쿠폰 중복 발급 실패")
    void coupon_중복_발급_실패() {
        // given
        couponService.issueCoupon(user.getId(), coupon.getId());

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(user.getId(), coupon.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_ALREADY_ISSUED);
    }

    @Test
    @DisplayName("쿠폰 품절 시 발급 실패")
    void coupon_품절_시_발급_실패() {
// given
        // 총 수량이 2개인 쿠폰 생성
        Coupon limitedCoupon = couponService.createCoupon(
                "한정 쿠폰",
                30,
                2,  // 총 2개만 발급 가능
                30,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );

        // 2명의 유저 생성
        User user2 = userRepository.save(User.create("유저2"));
        User user3 = userRepository.save(User.create("유저3"));

        // 2개 모두 발급
        couponService.issueCoupon(user.getId(), limitedCoupon.getId());
        couponService.issueCoupon(user2.getId(), limitedCoupon.getId());

        // when & then
        // 3번째 발급 시도 시 실패
        assertThatThrownBy(() -> couponService.issueCoupon(user3.getId(), limitedCoupon.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_SOLD_OUT);
    }

    @Test
    @DisplayName("만료된 쿠폰 발급 실패")
    void coupon_만료된_쿠폰_발급_실패() {
        // given
        // 이미 만료된 쿠폰 생성 (어제 만료)
        Coupon expiredCoupon = couponService.createCoupon(
                "만료된 쿠폰",
                15,
                10,
                1,
                LocalDateTime.now().minusDays(10),  // 10일 전 시작
                LocalDateTime.now().minusDays(1)    // 어제 만료
        );

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(user.getId(), expiredCoupon.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_AVAILABLE);
    }
    @Test
    @DisplayName("쿠폰 사용 성공")
    void coupon_사용_성공() {
        // given
        UserCoupon userCoupon = couponService.issueCoupon(user.getId(), coupon.getId());
        assertThat(userCoupon.getStatus()).isEqualTo(CouponStatus.AVAILABLE);

        // when
        userCoupon.use();
        userCouponRepository.save(userCoupon);

        // then
        UserCoupon usedCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertAll(
                () -> assertThat(usedCoupon.getStatus()).isEqualTo(CouponStatus.USED),
                () -> assertThat(usedCoupon.getUsedAt()).isNotNull()
        );
    }

    @Test
    @DisplayName("이미 사용한 쿠폰 재사용 실패")
    void coupon_이미_사용한_쿠폰_재사용_실패() {
        // given
        UserCoupon userCoupon = couponService.issueCoupon(user.getId(), coupon.getId());
        userCoupon.use();
        userCouponRepository.save(userCoupon);

        // when & then
        // use() 메서드 내부에서 BusinessException 발생
        assertThatThrownBy(() -> userCoupon.use())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("만료된 쿠폰 사용 실패")
    void coupon_만료된_쿠폰_사용_실패() {
        // given
        // 먼저 유효한 쿠폰으로 발급
        UserCoupon userCoupon = couponService.issueCoupon(user.getId(), coupon.getId());

        // 강제로 만료 처리
        userCoupon.expire();
        userCouponRepository.save(userCoupon);

        // when & then
        // 만료된 쿠폰 사용 시도
        assertThatThrownBy(() -> userCoupon.use())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("사용 가능한 쿠폰 목록 조회")
    void coupon_사용_가능한_쿠폰_목록_조회() {
        // given
        // 여러 개 쿠폰 발급
        UserCoupon userCoupon1 = couponService.issueCoupon(user.getId(), coupon.getId());

        Coupon coupon2 = couponService.createCoupon(
                "30% 할인 쿠폰",
                30,
                10,
                30,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        User user2 = userRepository.save(User.create("유저2"));
        couponService.issueCoupon(user2.getId(), coupon2.getId());

        // when
        List<UserCoupon> availableCoupons = couponService.getAvailableUserCoupons(user.getId());

        // then
        assertThat(availableCoupons).hasSize(1);
        assertThat(availableCoupons.get(0).getCouponId()).isEqualTo(coupon.getId());
    }

    @Test
    @DisplayName("발급 가능한 쿠폰 목록 조회")
    void coupon_발급_가능한_쿠폰_목록_조회() {
        // given
        Coupon coupon2 = couponService.createCoupon(
                "30% 할인 쿠폰",
                30,
                5,
                30,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );

        // 만료된 쿠폰
        couponService.createCoupon(
                "만료 쿠폰",
                40,
                10,
                1,
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(1)
        );

        // when
        List<Coupon> availableCoupons = couponService.getAvailableCoupons();

        // then
        // 만료되지 않고 발급 가능한 쿠폰만 조회됨
        assertThat(availableCoupons.size()).isGreaterThanOrEqualTo(2);
        assertThat(availableCoupons).extracting("name")
                .containsAnyOf("20% 할인쿠폰", "30% 할인 쿠폰");
    }
}
