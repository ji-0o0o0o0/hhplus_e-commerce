package com.hhplus.hhplus_ecommerce.product.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.product.application.ProductService;
import com.hhplus.hhplus_ecommerce.product.dto.response.PopularProductsResponse;
import com.hhplus.hhplus_ecommerce.product.dto.response.ProductDetailResponse;
import com.hhplus.hhplus_ecommerce.product.dto.response.ProductListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController implements ProductApi {

    private final ProductService productService;


    @Override
    public ResponseEntity<ApiResponse<ProductListResponse>> getProducts(Integer page, Integer size, String keyword) {
        ProductListResponse response = productService.getProductsWithPaging(page, size, keyword);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(Long productId) {
        ProductDetailResponse response = productService.getProductDetail(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<PopularProductsResponse>> getPopularProducts(Integer days) {
        PopularProductsResponse response = productService.getPopularProductsResponse();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 성능 비교용 (DB)
    @GetMapping("/popular/db")
    public ResponseEntity<ApiResponse<PopularProductsResponse>> getPopularProductsWithDB() {
        PopularProductsResponse response = productService.getPopularProductsResponseWithDB();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 성능 비교 API (둘 다 호출해서 시간 측정)
    @GetMapping("/popular/compare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> comparePerformance() {
        long dbStartTime = System.currentTimeMillis();
        PopularProductsResponse dbResponse = productService.getPopularProductsResponseWithDB();
        long dbEndTime = System.currentTimeMillis();

        long redisStartTime = System.currentTimeMillis();
        PopularProductsResponse redisResponse = productService.getPopularProductsResponse();
        long redisEndTime = System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("db_time_ms", dbEndTime - dbStartTime);
        result.put("redis_time_ms", redisEndTime - redisStartTime);
        result.put("improvement", (dbEndTime - dbStartTime) - (redisEndTime - redisStartTime));

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}