package com.hhplus.hhplus_ecommerce.coupon.repository;

import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {

    Coupon save(Coupon coupon);
    Optional<Coupon> findById(Long id);
    List<Coupon> findAll();
    List<Coupon> findAvailableCoupons();
}