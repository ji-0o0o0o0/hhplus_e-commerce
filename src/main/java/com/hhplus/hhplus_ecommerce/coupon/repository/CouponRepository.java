package com.hhplus.hhplus_ecommerce.coupon.repository;

import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon,Long> {

    @Query("SELECT c FROM Coupon c " +
            "WHERE c.issuedQuantity < c.totalQuantity " +
            "AND CURRENT_TIMESTAMP BETWEEN c.startDate AND c.endDate")
    List<Coupon> findAvailableCoupons();
}