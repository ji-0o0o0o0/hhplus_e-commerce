package com.hhplus.hhplus_ecommerce.product.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.product.dto.response.PopularProductsResponse;
import com.hhplus.hhplus_ecommerce.product.dto.response.ProductDetailResponse;
import com.hhplus.hhplus_ecommerce.product.dto.response.ProductListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 상품 API 명세
 * - Swagger 문서화를 위한 인터페이스
 * - API 명세와 실제 구현을 분리
 */
@Tag(name = "상품 API", description = "상품 조회 관련 API")
public interface ProductApi {

    /**
     * 상품 목록 조회
     */
    @GetMapping
    @Operation(summary = "상품 목록 조회", description = "페이징과 검색을 지원하는 상품 목록 조회 API")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "data": {
                                    "products": [
                                      {
                                        "productId": 1,
                                        "name": "Macbook Pro",
                                        "price": 2000000,
                                        "stock": 10,
                                        "category": "전자제품"
                                      }
                                    ],
                                    "totalElements": 100,
                                    "totalPages": 10,
                                    "currentPage": 0,
                                    "pageSize": 10
                                  }
                                }
                                """))
            )
    })
    ResponseEntity<ApiResponse<ProductListResponse>> getProducts(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") Integer size,

            @Parameter(description = "상품명 검색어", example = "macbook")
            @RequestParam(required = false) String keyword
    );

    /**
     * 상품 상세 조회
     */
    @GetMapping("/{productId}")
    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상품 정보를 조회합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "data": {
                                    "productId": 1,
                                    "name": "Macbook Pro",
                                    "description": "고성능 노트북",
                                    "price": 2000000,
                                    "stock": 10,
                                    "category": "전자제품"
                                  }
                                }
                                """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "상품을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "code": 404,
                                  "message": "상품을 찾을 수 없습니다."
                                }
                                """))
            )
    })
    ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(
            @Parameter(description = "상품 ID", example = "1", required = true)
            @PathVariable Long productId
    );

    /**
     * 인기 상품 조회
     */
    @GetMapping("/popular")
    @Operation(summary = "인기 상품 조회", description = "최근 N일간 판매량 기준 상위 5개 상품을 조회합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "data": {
                                    "products": [
                                      {
                                        "productId": 1,
                                        "name": "Macbook Pro",
                                        "price": 2000000,
                                        "category": "전자제품",
                                        "salesCount": 150
                                      }
                                    ]
                                  }
                                }
                                """))
            )
    })
    ResponseEntity<ApiResponse<PopularProductsResponse>> getPopularProducts(
            @Parameter(description = "집계 기간 (일)", example = "3")
            @RequestParam(defaultValue = "3") Integer days
    );
}