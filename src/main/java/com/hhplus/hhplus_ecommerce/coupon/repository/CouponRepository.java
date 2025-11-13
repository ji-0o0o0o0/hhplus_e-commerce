package com.hhplus.hhplus_ecommerce.coupon.repository;

import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon,Long> {

    List<Coupon> findAvailableCoupons();
}