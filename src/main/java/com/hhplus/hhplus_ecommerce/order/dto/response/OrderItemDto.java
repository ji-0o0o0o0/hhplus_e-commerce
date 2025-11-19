package com.hhplus.hhplus_ecommerce.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "주문 상품 정보")
public record OrderItemDto (

    @Schema(description = "주문 항목 ID", example = "1")
     Long orderItemId,

    @Schema(description = "상품 ID", example = "1")
     Long productId,

    @Schema(description = "상품명", example = "Macbook Pro")
     String productName,

    @Schema(description = "수량", example = "1")
     Integer quantity,

    @Schema(description = "단가", example = "2000000")
     Integer unitPrice,

    @Schema(description = "소계", example = "2000000")
     Integer subtotal
){}