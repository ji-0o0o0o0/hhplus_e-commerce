package com.hhplus.hhplus_ecommerce.coupon.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.common.lock.LockManager;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final LockManager lockManager;

    //선착순 코폰 발급
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        // LockManager를 통한 동시성 제어
        return lockManager.executeWithLock("coupon:" + couponId, () -> {
            // 1. 쿠폰 조회
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

            // 2. 중복 발급 확인
            userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                    .ifPresent(uc -> {
                        throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
                    });

            // 3. 발급 가능 여부 확인 (수량)
            if (!coupon.canIssue()) {
                throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
            }

            // 4. 유효기간 확인
            if (!coupon.isValid()) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
            }

            // 5. 쿠폰 발급 수량 증가
            coupon.increaseIssuedQuantity();
            couponRepository.save(coupon);

            // 6. 사용자 쿠폰 생성
            UserCoupon userCoupon = UserCoupon.issue(userId, coupon);
            return userCouponRepository.save(userCoupon);
        });
    }

    //사용자의 쿠폰 목록 조회
    public List<UserCoupon> getUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);

        // 만료된 쿠폰 처리
        LocalDateTime now = LocalDateTime.now();
        for (UserCoupon userCoupon : userCoupons) {
            if (userCoupon.getStatus() == CouponStatus.AVAILABLE &&
                    userCoupon.getExpiresAt() != null &&
                    now.isAfter(userCoupon.getExpiresAt())) {
                userCoupon.expire();
                userCouponRepository.save(userCoupon);
            }
        }

        return userCoupons;
    }

    //사용 가능한 쿠폰 목록 조회
    public List<UserCoupon> getAvailableUserCoupons(Long userId) {
        return userCouponRepository.findByUserIdAndStatus(userId, CouponStatus.AVAILABLE);
    }

    //발급 가능한 쿠폰 목록 조회
    public List<Coupon> getAvailableCoupons() {
        return couponRepository.findAvailableCoupons();
    }

    //쿠폰 조회
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
    }

    // 쿠폰 생성 (테스트용)
    public Coupon createCoupon(String name, Integer discountRate, Integer totalQuantity,Integer validityDays,
                               LocalDateTime startDate, LocalDateTime endDate) {
        Coupon coupon = Coupon.create(name, discountRate, totalQuantity,validityDays, startDate, endDate);
        return couponRepository.save(coupon);
    }
}
