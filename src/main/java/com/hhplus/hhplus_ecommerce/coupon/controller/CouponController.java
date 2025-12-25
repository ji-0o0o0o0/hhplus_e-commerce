package com.hhplus.hhplus_ecommerce.coupon.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.application.CouponRedisService;
import com.hhplus.hhplus_ecommerce.coupon.application.CouponService;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.dto.request.IssueCouponRequest;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponIssueAsyncResponse;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponIssueResponse;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponIssueStatusResponse;
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

    /**
     * Kafka 기반 쿠폰 비동기 발급
     * - Redis 기반 빠른 재고 확인
     * - Kafka 메시지 발행 후 즉시 응답 (202 Accepted)
     * - Consumer가 실제 발급 처리
     * - requestId로 멱등성 보장
     */
    @PostMapping("/{couponId}/issue-kafka")
    public ResponseEntity<ApiResponse<CouponIssueAsyncResponse>> issueCouponKafka(
            @PathVariable Long couponId,
            @RequestBody IssueCouponRequest request
    ) {
        String requestId = couponService.issueCouponAsync(request.userId(), couponId);

        CouponIssueAsyncResponse response = new CouponIssueAsyncResponse(
                requestId,
                request.userId(),
                couponId,
                "쿠폰 발급 요청이 접수되었습니다. 처리 완료까지 다소 시간이 소요될 수 있습니다."
        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<CouponListResponse>> getUserCoupons(Long userId, CouponStatus status) {
        CouponListResponse response = couponService.getUserCouponsWithDetails(userId, status);
        return ResponseEntity.ok(ApiResponse.success(response));

    }

    /**
     * 비동기 쿠폰 발급 상태 조회
     * - requestId로 발급 결과 확인
     * - 발급 완료 여부와 발급된 쿠폰 정보 반환
     */
    @GetMapping("/issue-status/{requestId}")
    public ResponseEntity<ApiResponse<CouponIssueStatusResponse>> getCouponIssueStatus(
            @PathVariable String requestId
    ) {
        CouponIssueStatusResponse response =
                couponService.getCouponIssueStatus(requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
