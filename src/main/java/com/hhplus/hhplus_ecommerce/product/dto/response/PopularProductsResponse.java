package com.hhplus.hhplus_ecommerce.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Schema(description = "인기 상품 조회 응답")
public record PopularProductsResponse (

    @Schema(description = "인기 상품 목록 (최대 5개)")
    List<PopularProductDto> products
){}