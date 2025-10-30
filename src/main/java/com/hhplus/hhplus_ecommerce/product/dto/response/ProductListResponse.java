package com.hhplus.hhplus_ecommerce.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "상품 목록 조회 응답")
public class ProductListResponse {

    @Schema(description = "상품 목록")
    private List<ProductDto> products;

    @Schema(description = "전체 상품 수", example = "100")
    private Long totalElements;

    @Schema(description = "전체 페이지 수", example = "10")
    private Integer totalPages;

    @Schema(description = "현재 페이지", example = "0")
    private Integer currentPage;

    @Schema(description = "페이지 크기", example = "10")
    private Integer size;
}
