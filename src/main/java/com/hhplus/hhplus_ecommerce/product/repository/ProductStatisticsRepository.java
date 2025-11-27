package com.hhplus.hhplus_ecommerce.product.repository;

import com.hhplus.hhplus_ecommerce.product.domain.ProductStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductStatisticsRepository extends JpaRepository<ProductStatistics, Long> {

    @Query(value ="SELECT ps.productId, SUM(ps.salesCount) as totalSales " +
            "FROM ProductStatistics ps " +
            "WHERE ps.statsDate >= :startDate " +
            "GROUP BY ps.productId " +
            "ORDER BY totalSales DESC"+
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopSellingProductIds(@Param("startDate") LocalDate startDate, @Param("limit") int limit);


}
