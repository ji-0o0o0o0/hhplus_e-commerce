package com.hhplus.hhplus_ecommerce.coupon.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponTransactionService {
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public UserCoupon issueCouponTransaction(Long userId, Long couponId) {
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
    }

}
