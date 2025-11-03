package com.hhplus.hhplus_ecommerce.coupon.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.dto.request.IssueCouponRequest;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponIssueResponse;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 쿠폰 API 명세
 * - Swagger 문서화를 위한 인터페이스
 * - API 명세와 실제 구현을 분리
 */
@Tag(name = "쿠폰 API", description = "쿠폰 발급 및 조회 API")
public interface CouponApi {

    /**
     * 쿠폰 발급
     */
    @PostMapping("/issue")
    @Operation(summary = "쿠폰 발급", description = "선착순으로 사용자에게 쿠폰을 발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "쿠폰 발급 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "message": "쿠폰이 성공적으로 발급되었습니다",
                                  "data": {
                                    "userCouponId": 1,
                                    "userId": 1,
                                    "couponId": 1,
                                    "couponName": "20% 할인 쿠폰",
                                    "discountRate": 20,
                                    "status": "AVAILABLE",
                                    "issuedAt": "2024-11-03T18:00:00",
                                    "expiresAt": "2024-11-04T18:00:00"
                                  }
                                }
                                """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "쿠폰을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "code": 404,
                                  "message": "쿠폰을 찾을 수 없습니다."
                                }
                                """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "발급 불가 (이미 발급받음 또는 쿠폰 소진)",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "이미 발급받음", value = """
                                        {
                                          "success": false,
                                          "code": 409,
                                          "message": "이미 발급받은 쿠폰입니다."
                                        }
                                        """),
                                    @ExampleObject(name = "쿠폰 소진", value = """
                                        {
                                          "success": false,
                                          "code": 409,
                                          "message": "쿠폰이 모두 소진되었습니다."
                                        }
                                        """)
                            })
            )
    })
    ResponseEntity<ApiResponse<CouponIssueResponse>> issueCoupon(
            @Valid @RequestBody IssueCouponRequest request
    );

    /**
     * 보유 쿠폰 조회
     */
    @GetMapping
    @Operation(summary = "보유 쿠폰 조회", description = "사용자에게 발급된 쿠폰 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "data": {
                                    "coupons": [
                                      {
                                        "userCouponId": 1,
                                        "couponId": 1,
                                        "couponName": "20% 할인 쿠폰",
                                        "discountRate": 20,
                                        "status": "AVAILABLE",
                                        "issuedAt": "2024-11-01T18:00:00",
                                        "usedAt": null,
                                        "expiresAt": "2024-11-03T18:00:00"
                                      }
                                    ]
                                  }
                                }
                                """))
            )
    })
    ResponseEntity<ApiResponse<CouponListResponse>> getCoupons(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long userId,

            @Parameter(description = "쿠폰 상태 필터 (AVAILABLE, USED, EXPIRED)")
            @RequestParam(required = false) CouponStatus status
    );
}