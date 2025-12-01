package com.hhplus.hhplus_ecommerce.product.repository;

import com.hhplus.hhplus_ecommerce.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(String category);

}