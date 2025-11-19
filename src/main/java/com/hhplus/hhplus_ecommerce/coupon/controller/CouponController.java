package com.hhplus.hhplus_ecommerce.coupon.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.dto.request.IssueCouponRequest;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponIssueResponse;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponListResponse;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.UserCouponDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController implements CouponApi {

    @Override
    public ResponseEntity<ApiResponse<CouponIssueResponse>> issueCoupon(IssueCouponRequest request) {
        // Mock: 특정 쿠폰 ID는 없는 것으로 처리
        if (request.couponId() == 999L) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "쿠폰을 찾을 수 없습니다."));
        }

        // Mock: 특정 쿠폰 ID는 소진된 것으로 처리
        if (request.couponId() == 888L) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(409, "쿠폰이 모두 소진되었습니다."));
        }

        // Mock: 특정 사용자 ID는 이미 발급받은 것으로 처리
        if (request.userId() == 777L) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(409, "이미 발급받은 쿠폰입니다."));
        }

        CouponIssueResponse response = new CouponIssueResponse(
                1L,
                request.userId(),
                request.couponId(),
                "20% 할인 쿠폰",
                20,
                CouponStatus.AVAILABLE
                , LocalDateTime.now(),
                LocalDateTime.now().plusDays(1)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("쿠폰이 성공적으로 발급되었습니다", response));
    }

    @Override
    public ResponseEntity<ApiResponse<CouponListResponse>> getCoupons(Long userId, CouponStatus status) {
        List<UserCouponDto> userCouponList = List.of(
                new UserCouponDto(1L,1L,"20% 할인 쿠폰",20, CouponStatus.AVAILABLE,LocalDateTime.now().minusDays(2),LocalDateTime.now(),LocalDateTime.now()),
                new UserCouponDto(2L,2L,"10% 할인 쿠폰",10,CouponStatus.USED,LocalDateTime.now().minusDays(3),LocalDateTime.now(),LocalDateTime.now().plusDays(2))
        );

        CouponListResponse  response = new CouponListResponse(
                userCouponList
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
