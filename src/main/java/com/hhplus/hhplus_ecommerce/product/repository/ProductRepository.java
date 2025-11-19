package com.hhplus.hhplus_ecommerce.product.repository;

import com.hhplus.hhplus_ecommerce.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(String category);

    @Query(value = "SELECT p.* FROM products p " +
            "JOIN product_statistics ps ON p.id = ps.product_id " +
            "WHERE ps.stats_date >= DATE(:startDate) " +
            "GROUP BY p.id " +
            "ORDER BY SUM(ps.sales_count) DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Product> findTopSellingProducts(@Param("startDate") LocalDateTime startDate, @Param("limit") int limit);
}