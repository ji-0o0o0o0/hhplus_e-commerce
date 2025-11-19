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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final LockManager lockManager;

    @Transactional
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        int maxRetries = 5;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                Coupon coupon = couponRepository.findById(couponId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

                // 중복 발급 체크
                userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                        .ifPresent(uc -> {
                            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
                        });

                // 발급 가능 여부 체크
                if (!coupon.canIssue()) {
                    throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
                }

                // 유효기간 체크
                if (!coupon.isValid()) {
                    throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
                }

                // 발급 수량 증가 (낙관적 락)
                coupon.increaseIssuedQuantity();
                couponRepository.save(coupon);

                // 사용자 쿠폰 발급
                UserCoupon userCoupon = UserCoupon.issue(userId, coupon);
                return userCouponRepository.save(userCoupon);

            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
                }
                // 짧은 대기 후 재시도
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(ErrorCode.LOCK_INTERRUPTED);
                }
            }
        }

        throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
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
