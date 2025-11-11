package com.hhplus.hhplus_ecommerce.coupon.repository;

import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {

    UserCoupon save(UserCoupon userCoupon);
    Optional<UserCoupon> findById(Long id);
    List<UserCoupon> findByUserId(Long userId);
    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
    List<UserCoupon> findByUserIdAndStatus(Long userId, CouponStatus status);
}