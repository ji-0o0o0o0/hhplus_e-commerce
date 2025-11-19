package com.hhplus.hhplus_ecommerce.cart.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "장바구니 조회 응답")
public record CartListResponse (
        @Schema(description = "장바구니 항목 목록")
         List<CartDto> cartItems,

        @Schema(description = "총 금액", example = "5200000")
         Integer totalAmount
){}