package com.hhplus.hhplus_ecommerce.point.repository;

import com.hhplus.hhplus_ecommerce.point.domain.Point;
import com.hhplus.hhplus_ecommerce.point.domain.PointTransaction;

import java.util.List;
import java.util.Optional;

public interface PointRepository {

    Point save(Point point);
    Optional<Point> findById(Long id);
    Optional<Point> findByUserId(Long userId);
}