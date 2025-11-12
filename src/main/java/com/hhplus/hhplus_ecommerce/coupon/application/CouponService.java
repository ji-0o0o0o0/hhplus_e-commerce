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

    public UserCoupon issueCoupon(Long userId, Long couponId) {
        return lockManager.executeWithLock("coupon:" + couponId, () -> {
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

            userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                    .ifPresent(uc -> {
                        throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
                    });

            if (!coupon.canIssue()) {
                throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
            }

            if (!coupon.isValid()) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
            }

            coupon.increaseIssuedQuantity();
            couponRepository.save(coupon);

            UserCoupon userCoupon = UserCoupon.issue(userId, coupon);
            return userCouponRepository.save(userCoupon);
        });
    }

    public List<UserCoupon> getUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);

        // 만료된 쿠폰 처리
        for (UserCoupon userCoupon : userCoupons) {
            if (userCoupon.shouldExpire()) {
                userCoupon.expire();
                userCouponRepository.save(userCoupon);
            }
        }

        return userCoupons;
    }

    public List<UserCoupon> getAvailableUserCoupons(Long userId) {
        return userCouponRepository.findByUserIdAndStatus(userId, CouponStatus.AVAILABLE);
    }

    public List<Coupon> getAvailableCoupons() {
        return couponRepository.findAvailableCoupons();
    }

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
