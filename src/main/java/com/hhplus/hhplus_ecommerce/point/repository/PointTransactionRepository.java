package com.hhplus.hhplus_ecommerce.point.repository;

import com.hhplus.hhplus_ecommerce.point.domain.PointTransaction;

import java.util.List;

public interface PointTransactionRepository {

    PointTransaction save(PointTransaction transaction);
    List<PointTransaction> findByPointId(Long pointId);
    List<PointTransaction> findByPointId(Long pointId, int offset, int limit);
}