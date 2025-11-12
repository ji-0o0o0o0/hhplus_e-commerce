package com.hhplus.hhplus_ecommerce.point.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.point.TransactionType;
import com.hhplus.hhplus_ecommerce.point.domain.Point;
import com.hhplus.hhplus_ecommerce.point.domain.PointTransaction;
import com.hhplus.hhplus_ecommerce.point.repository.PointRepository;
import com.hhplus.hhplus_ecommerce.point.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {
    private final PointRepository pointRepository;
    private final PointTransactionRepository pointTransactionRepository;
    public Point getPoint(Long userId) {
        return pointRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Point 없으면 0원으로 생성
                    Point newPoint = Point.create(userId);
                    return pointRepository.save(newPoint);
                });
    }
    public Point changePoint(Long userId, Integer amount) {
        Point point = getPoint(userId);
        point.charge(amount);
        Point savedPoint = pointRepository.save(point);
        PointTransaction transaction = PointTransaction.create(userId,amount, TransactionType.CHARGE,savedPoint.getAmount());
        pointTransactionRepository.save(transaction);

        return savedPoint;
    }
    public Point usePoint(Long userId, Integer amount) {
        Point point = getPoint(userId);
        point.use(amount);
        Point savedPoint = pointRepository.save(point);
        PointTransaction transaction = PointTransaction.create(userId,amount, TransactionType.USE,savedPoint.getAmount());
        pointTransactionRepository.save(transaction);

        return  savedPoint;
    }

    public List<PointTransaction> getPointTransactions(Long userId) {
        return pointTransactionRepository.findByUserId(userId);
    }
}
