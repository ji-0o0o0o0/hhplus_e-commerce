package com.hhplus.hhplus_ecommerce.product.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.common.lock.LockManager;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final LockManager lockManager;

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

    //재고 차감 (동시성 제어 적용)
    public void decreaseStock(Long productId, Integer quantity) {
        lockManager.executeWithLock("product:" + productId, () -> {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

            product.decreaseStock(quantity);
            productRepository.save(product);
        });
    }

    //재고 증가 (동시성 제어 적용)
    public void increaseStock(Long productId, Integer quantity) {
        lockManager.executeWithLock("product:" + productId, () -> {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

            product.increaseStock(quantity);
            productRepository.save(product);
        });
    }
}