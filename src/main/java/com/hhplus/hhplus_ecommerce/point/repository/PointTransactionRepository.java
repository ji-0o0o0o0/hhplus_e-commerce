package com.hhplus.hhplus_ecommerce.point.repository;

import com.hhplus.hhplus_ecommerce.point.domain.PointTransaction;

import java.util.List;

public interface PointTransactionRepository {

    PointTransaction save(PointTransaction transaction);
    List<PointTransaction> findByUserId(Long userId);
    List<PointTransaction> findByUserId(Long userId, int offset, int limit);
}