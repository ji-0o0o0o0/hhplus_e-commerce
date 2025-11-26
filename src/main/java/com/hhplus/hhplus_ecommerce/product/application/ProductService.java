package com.hhplus.hhplus_ecommerce.product.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.dto.response.*;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> getTopProducts() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(3);
        return productRepository.findTopSellingProducts(startDate, 5);
    }

    public ProductListResponse getProductsWithPaging(Integer page, Integer size, String keyword) {
        List<Product> products = productRepository.findAll();  // 페이징/검색은 나중에 개선

        List<ProductDto> productDtos = products.stream()
                .map(p -> new ProductDto(
                        p.getId(),
                        p.getName(),
                        p.getPrice(),
                        p.getStock(),
                        p.getCategory()
                ))
                .toList();

        return new ProductListResponse(
                productDtos,
                (long) products.size(),
                (products.size() + size - 1) / size,
                page,
                size
        );
    }

    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = getProduct(productId);

        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory()
        );
    }

    public PopularProductsResponse getPopularProductsResponse() {
        List<Product> products = getTopProducts();

        List<PopularProductDto> popularProducts = products.stream()
                .map(p -> new PopularProductDto(
                        p.getId(),
                        p.getName(),
                        p.getPrice(),
                        p.getCategory(),
                        0  // TODO: 판매량 정보 추가 필요
                ))
                .toList();

        return new PopularProductsResponse(popularProducts);
    }



}