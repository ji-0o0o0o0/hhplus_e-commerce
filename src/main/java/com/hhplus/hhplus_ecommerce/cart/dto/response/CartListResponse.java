package com.hhplus.hhplus_ecommerce.cart.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "장바구니 조회 응답")
public class CartListResponse {

    @Schema(description = "장바구니 항목 목록")
    private List<CartDto> cartItems;

    @Schema(description = "총 금액", example = "5200000")
    private Integer totalAmount;
}