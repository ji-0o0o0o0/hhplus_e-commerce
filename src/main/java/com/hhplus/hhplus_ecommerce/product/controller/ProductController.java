package com.hhplus.hhplus_ecommerce.product.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.product.dto.response.PopularProductDto;
import com.hhplus.hhplus_ecommerce.product.dto.response.ProductDto;
import com.hhplus.hhplus_ecommerce.product.dto.response.PopularProductsResponse;
import com.hhplus.hhplus_ecommerce.product.dto.response.ProductDetailResponse;
import com.hhplus.hhplus_ecommerce.product.dto.response.ProductListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController implements ProductApi {


    @Override
    public ResponseEntity<ApiResponse<ProductListResponse>> getProducts(
            Integer page, Integer size, String keyword) {
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

    @Override
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(Long productId) {
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

    @Override
    public ResponseEntity<ApiResponse<PopularProductsResponse>> getPopularProducts(Integer days) {
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