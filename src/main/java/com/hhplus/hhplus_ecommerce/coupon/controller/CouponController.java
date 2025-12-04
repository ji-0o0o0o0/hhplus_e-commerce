package com.hhplus.hhplus_ecommerce.coupon.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.application.CouponRedisService;
import com.hhplus.hhplus_ecommerce.coupon.application.CouponService;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.dto.request.IssueCouponRequest;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponIssueResponse;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponListResponse;
import com.hhplus.hhplus_ecommerce.coupon.repository.RedisCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController implements CouponApi {

    private final CouponService couponService;
    private final CouponRedisService couponRedisService;

    @Override
    public ResponseEntity<ApiResponse<CouponIssueResponse>> issueCoupon(IssueCouponRequest request) {
        UserCoupon issueCoupon = couponService.issueCouponWithDistributedLock(request.userId(), request.couponId());
        CouponIssueResponse response = couponService.issueCouponWithResponse(issueCoupon);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("쿠폰이 성공적으로 발급되었습니다", response));

    }
    @PostMapping("/{couponId}/issue/async")
    public  ResponseEntity<ApiResponse<CouponIssueResponse>> issueCouponAsync(
            @PathVariable Long couponId,
            @RequestBody RedisCouponRepository.CouponIssueRequest request
    ) {
        couponRedisService.requestCouponAsync(request.userId(), couponId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("대기열에 추가되었습니다",null));
    }


    @Override
    public ResponseEntity<ApiResponse<CouponListResponse>> getUserCoupons(Long userId, CouponStatus status) {
        CouponListResponse response = couponService.getUserCouponsWithDetails(userId, status);
        return ResponseEntity.ok(ApiResponse.success(response));

    }

}
