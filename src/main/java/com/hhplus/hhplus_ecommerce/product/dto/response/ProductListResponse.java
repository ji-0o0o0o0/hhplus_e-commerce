package com.hhplus.hhplus_ecommerce.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Schema(description = "상품 목록 조회 응답")
public record ProductListResponse (

    @Schema(description = "상품 목록")
    List<ProductDto> products,

    @Schema(description = "전체 상품 수", example = "100")
    Long totalElements,

    @Schema(description = "전체 페이지 수", example = "10")
    Integer totalPages,

    @Schema(description = "현재 페이지", example = "0")
    Integer currentPage,

    @Schema(description = "페이지 크기", example = "10")
    Integer size
){}
