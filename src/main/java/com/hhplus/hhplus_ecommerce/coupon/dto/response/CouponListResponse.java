package com.hhplus.hhplus_ecommerce.coupon.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Schema(description = "쿠폰 목록 조회 응답")
public record CouponListResponse (

    @Schema(description = "사용자 쿠폰 목록")
    List<UserCouponDto> coupons
){}
