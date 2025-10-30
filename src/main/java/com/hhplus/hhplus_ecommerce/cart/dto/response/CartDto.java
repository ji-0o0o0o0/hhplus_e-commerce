package com.hhplus.hhplus_ecommerce.cart.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "장바구니 항목 정보")
public class CartDto {

    @Schema(description = "장바구니 항목 ID", example = "1")
    private Long cartItemId;

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "Macbook Pro")
    private String productName;

    @Schema(description = "수량", example = "2")
    private Integer quantity;

    @Schema(description = "상품 단가", example = "2000000")
    private Integer price;

    @Schema(description = "소계", example = "4000000")
    private Integer subtotal;
}