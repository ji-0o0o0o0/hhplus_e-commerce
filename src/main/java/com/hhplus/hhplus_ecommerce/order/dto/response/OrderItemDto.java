package com.hhplus.hhplus_ecommerce.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "주문 상품 정보")
public class OrderItemDto {

    @Schema(description = "주문 항목 ID", example = "1")
    private Long orderItemId;

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "상품명", example = "Macbook Pro")
    private String productName;

    @Schema(description = "수량", example = "1")
    private Integer quantity;

    @Schema(description = "단가", example = "2000000")
    private Integer unitPrice;

    @Schema(description = "소계", example = "2000000")
    private Integer subtotal;
}