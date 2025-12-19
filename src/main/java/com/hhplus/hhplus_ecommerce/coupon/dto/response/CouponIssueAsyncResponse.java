package com.hhplus.hhplus_ecommerce.coupon.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "쿠폰 비동기 발급 응답 (Kafka)")
public record CouponIssueAsyncResponse(

    @Schema(description = "발급 요청 ID (멱등성 키)", example = "550e8400-e29b-41d4-a716-446655440000")
    String requestId,

    @Schema(description = "사용자 ID", example = "1")
    Long userId,

    @Schema(description = "쿠폰 ID", example = "1")
    Long couponId,

    @Schema(description = "응답 메시지", example = "쿠폰 발급 요청이 접수되었습니다. 처리 완료까지 다소 시간이 소요될 수 있습니다.")
    String message
) {
}
