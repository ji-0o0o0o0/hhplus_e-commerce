package com.hhplus.hhplus_ecommerce.product.repository;

import java.util.List;
import java.util.Map;

public interface ProductRankingRepository {
    void incrementSales(Long productId, int quantity);
    List<Long> getTopProductIds(int limit);
    Long getSalesCount(Long productId);
    Long getRank(Long productId);

    Map<Long, Long> getSalesCountBulk(List<Long> productIds);
    Map<Long, Long> getRankBulk(List<Long> productIds);
}