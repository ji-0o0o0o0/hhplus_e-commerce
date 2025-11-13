package com.hhplus.hhplus_ecommerce.cart.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장바구니 항목 정보")
public record CartDto (

    @Schema(description = "장바구니 항목 ID", example = "1")
     Long cartItemId,

    @Schema(description = "상품 ID", example = "1")
     Long productId,

    @Schema(description = "상품명", example = "Macbook Pro")
     String productName,

    @Schema(description = "수량", example = "2")
     Integer quantity,

    @Schema(description = "상품 단가", example = "2000000")
     Integer price,

    @Schema(description = "소계", example = "4000000")
     Integer subtotal
){}