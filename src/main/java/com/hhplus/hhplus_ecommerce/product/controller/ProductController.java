package com.hhplus.hhplus_ecommerce.product.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.product.dto.response.PopularProductDto;
import com.hhplus.hhplus_ecommerce.product.dto.response.ProductDto;
import com.hhplus.hhplus_ecommerce.product.dto.response.PopularProductsResponse;
import com.hhplus.hhplus_ecommerce.product.dto.response.ProductDetailResponse;
import com.hhplus.hhplus_ecommerce.product.dto.response.ProductListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "상품 API", description = "상품 조회 관련 API")
public class ProductController {

    @GetMapping
    @Operation(summary = "상품 목록 조회", description = "페이징과 검색을 지원하는 상품 목록 조회 API")
    public ResponseEntity<ApiResponse<ProductListResponse>> getProducts(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") Integer size,

            @Parameter(description = "상품명 검색어", example = "macbook")
            @RequestParam(required = false) String keyword
    ) {
        // Mock 데이터
        List<ProductDto> products = List.of(
                new ProductDto(1L, "Macbook Pro", 2000000, 10, "전자제품"),
                new ProductDto(2L, "iPhone 12", 1200000, 20, "전자제품")
        );

        ProductListResponse response = new ProductListResponse(
                products,
                100L,
                10,
                page,
                size
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상품 정보를 조회합니다")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(
            @Parameter(description = "상품 ID", example = "1", required = true)
            @PathVariable Long productId
    ) {
        // Mock 데이터
        ProductDetailResponse response = new ProductDetailResponse(
                productId,
                "Macbook Pro",
                "고성능 노트북",
                2000000,
                10,
                "전자제품"
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/popular")
    @Operation(summary = "인기 상품 조회", description = "최근 3일간 판매량 기준 상위 5개 상품을 조회합니다")
    public ResponseEntity<ApiResponse<PopularProductsResponse>> getPopularProducts(
            @Parameter(description = "집계 기간 (일)", example = "3")
            @RequestParam(defaultValue = "3") Integer days
    ) {
        // Mock 데이터
        List<PopularProductDto> products = List.of(
                new PopularProductDto(1L, "Macbook Pro", 2000000, "전자제품", 150),
                new PopularProductDto(2L, "iPhone 12", 1200000, "전자제품", 120),
                new PopularProductDto(3L, "AirPods Pro", 300000, "전자제품", 100),
                new PopularProductDto(4L, "iPad Air", 800000, "전자제품", 80),
                new PopularProductDto(5L, "Apple Watch", 500000, "전자제품", 60)
        );

        PopularProductsResponse response = new PopularProductsResponse(products);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}