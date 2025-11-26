package com.hhplus.hhplus_ecommerce.coupon.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.application.CouponService;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.dto.request.IssueCouponRequest;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponIssueResponse;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController implements CouponApi {

    private final CouponService couponService;

    @Override
    public ResponseEntity<ApiResponse<CouponIssueResponse>> issueCoupon(IssueCouponRequest request) {
        UserCoupon issueCoupon = couponService.issueCoupon(request.userId(), request.couponId());
        CouponIssueResponse response = couponService.issueCouponWithResponse(issueCoupon);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("쿠폰이 성공적으로 발급되었습니다", response));

    }

    @Override
    public ResponseEntity<ApiResponse<CouponListResponse>> getUserCoupons(Long userId, CouponStatus status) {
        CouponListResponse response = couponService.getUserCouponsWithDetails(userId, status);
        return ResponseEntity.ok(ApiResponse.success(response));

    }

}
