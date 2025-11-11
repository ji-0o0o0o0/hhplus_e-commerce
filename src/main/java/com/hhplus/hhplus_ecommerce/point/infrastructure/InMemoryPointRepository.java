package com.hhplus.hhplus_ecommerce.point.infrastructure;

import com.hhplus.hhplus_ecommerce.point.domain.Point;
import com.hhplus.hhplus_ecommerce.point.domain.PointTransaction;
import com.hhplus.hhplus_ecommerce.point.repository.PointRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryPointRepository implements PointRepository {

    private final Map<Long, Point> store = new ConcurrentHashMap<>();
    private final Map<Long, Point> userIdIndex = new ConcurrentHashMap<>();  // userId로 빠른 조회
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Point save(Point point) {
        if (point.getId() == null) {
            Point newPoint = Point.builder()
                    .id(idGenerator.getAndIncrement())
                    .userId(point.getUserId())
                    .amount(point.getAmount())
                    .updatedAt(point.getUpdatedAt())
                    .build();
            store.put(newPoint.getId(), newPoint);
            userIdIndex.put(newPoint.getUserId(), newPoint);
            return newPoint;
        } else {
            store.put(point.getId(), point);
            userIdIndex.put(point.getUserId(), point);
            return point;
        }
    }

    @Override
    public Optional<Point> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Point> findByUserId(Long userId) {
        return Optional.ofNullable(userIdIndex.get(userId));
    }

    public void clear() {
        store.clear();
        userIdIndex.clear();
        idGenerator.set(1);
    }
}