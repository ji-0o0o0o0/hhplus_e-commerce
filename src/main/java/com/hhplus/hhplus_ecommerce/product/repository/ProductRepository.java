package com.hhplus.hhplus_ecommerce.product.repository;

import com.hhplus.hhplus_ecommerce.product.domain.Product;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {

   
    Product save(Product product);
    Optional<Product> findById(Long id);
    List<Product> findAll();
    List<Product> findByCategory(String category);
    List<Product> findTopSellingProducts(LocalDateTime startDate, int limit);
}