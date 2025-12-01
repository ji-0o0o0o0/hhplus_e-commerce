package com.hhplus.hhplus_ecommerce.product.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.product.application.ProductService;
import com.hhplus.hhplus_ecommerce.product.dto.response.PopularProductsResponse;
import com.hhplus.hhplus_ecommerce.product.dto.response.ProductDetailResponse;
import com.hhplus.hhplus_ecommerce.product.dto.response.ProductListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}