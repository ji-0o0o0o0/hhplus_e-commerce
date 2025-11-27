package com.hhplus.hhplus_ecommerce.product.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.dto.response.*;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import com.hhplus.hhplus_ecommerce.product.repository.ProductStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductStatisticsRepository productStatisticsRepository;

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

    @Cacheable(value = "productDetail", key = "#productId", sync = true)
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

    @Cacheable(value = "popularProducts", key = "'top5'", sync = true)
    public PopularProductsResponse getPopularProductsResponse() {
        LocalDate startDate = LocalDate.now().minusDays(3);
        List<Object[]> topSellingData = productStatisticsRepository.findTopSellingProductIds(startDate, 5);

        List<PopularProductDto> popularProducts = new ArrayList<>();

        for (Object[] data : topSellingData) {
            Long productId = (Long) data[0];
            Long totalSales = (Long) data[1];

            Product product = getProduct(productId);

            popularProducts.add(new PopularProductDto(
                    productId,
                    product.getName(),
                    product.getPrice(),
                    product.getCategory(),
                    totalSales.intValue()
            ));
        }

        return new PopularProductsResponse(popularProducts);
    }



}