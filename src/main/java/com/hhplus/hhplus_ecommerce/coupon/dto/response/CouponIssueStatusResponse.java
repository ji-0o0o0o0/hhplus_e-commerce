package com.hhplus.hhplus_ecommerce.coupon.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비동기 쿠폰 발급 상태 조회 응답")
public record CouponIssueStatusResponse(

    @Schema(description = "발급 요청 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    String requestId,

    @Schema(description = "발급 상태", example = "COMPLETED", allowableValues = {"COMPLETED", "NOT_FOUND"})
    String status,

    @Schema(description = "사용자 ID", example = "1")
    Long userId,

    @Schema(description = "쿠폰 ID", example = "100")
    Long couponId,

    @Schema(description = "발급된 UserCoupon ID (완료 시)", example = "456")
    Long userCouponId,

    @Schema(description = "쿠폰 이름 (완료 시)", example = "신규 가입 10% 할인 쿠폰")
    String couponName,

    @Schema(description = "상태 메시지", example = "쿠폰 발급이 완료되었습니다")
    String message
) {

    public static CouponIssueStatusResponse completed(String requestId, Long userId, Long couponId,
                                                      Long userCouponId, String couponName) {
        return new CouponIssueStatusResponse(
                requestId,
                "COMPLETED",
                userId,
                couponId,
                userCouponId,
                couponName,
                "쿠폰 발급이 완료되었습니다"
        );
    }

    public static CouponIssueStatusResponse notFound(String requestId) {
        return new CouponIssueStatusResponse(
                requestId,
                "NOT_FOUND",
                null,
                null,
                null,
                null,
                "발급 요청을 찾을 수 없습니다"
        );
    }
}