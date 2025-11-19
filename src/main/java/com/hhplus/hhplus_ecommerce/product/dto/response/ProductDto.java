package com.hhplus.hhplus_ecommerce.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 정보")
public record ProductDto (

    @Schema(description = "상품 ID", example = "1")
    Long id,

    @Schema(description = "상품 이름", example = "Macbook Pro")
    String name,

    @Schema(description = "상품 가격", example = "2000000")
    Integer price,

    @Schema(description = "재고 수량", example = "10")
    Integer stock,

    @Schema(description = "카테고리", example = "전자제품")
    String category
){}
