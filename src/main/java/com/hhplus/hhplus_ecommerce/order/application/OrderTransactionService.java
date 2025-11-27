package com.hhplus.hhplus_ecommerce.order.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderTransactionService {
    private final ProductRepository productRepository;

    @Transactional
    @CacheEvict(value = "productDetail", key = "#productId")
    public Product decreaseProductStockTransaction(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.hasSufficientStock(quantity)) {
            throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
        }
        product.decreaseStock(quantity);
        return productRepository.save(product);
    }
    @Transactional
    @CacheEvict(value = "productDetail", key = "#productId")
    public void increaseProductStockTransaction(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.increaseStock(quantity);
        productRepository.save(product);
    }

}
