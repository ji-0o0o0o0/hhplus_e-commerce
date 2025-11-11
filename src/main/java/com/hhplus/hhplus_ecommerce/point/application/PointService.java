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
    //조회
    public Point getPoint(Long userId) {
        return pointRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Point 없으면 0원으로 생성
                    Point newPoint = Point.create(userId);
                    return pointRepository.save(newPoint);
                });
    }
    //충전
    public Point changePoint(Long userId, Integer amount) {
        //1. 조회
        Point point = getPoint(userId);
        //2. 포인트 충전
        point.charge(amount);
        //3. 포인트 충전 저장
        Point savedPoint = pointRepository.save(point);
        //4. 거래 내역 저장
        PointTransaction transaction = PointTransaction.create(userId,amount, TransactionType.CHARGE,savedPoint.getAmount());
        pointTransactionRepository.save(transaction);

        return savedPoint;
    }
    //사용
    public Point usePoint(Long userId, Integer amount) {
        //1. 포인트 조회
        Point point = getPoint(userId);
        //2. 포인트 사용
        point.use(amount);
        //3. 포인트 사용 저장
        Point savedPoint = pointRepository.save(point);
        //4. 포인트 사용 내역 저장
        PointTransaction transaction = PointTransaction.create(userId,amount, TransactionType.USE,savedPoint.getAmount());
        pointTransactionRepository.save(transaction);

        return  savedPoint;
    }

    //거래내용 조회
    public List<PointTransaction> getPointTransactions(Long userId) {
        return pointTransactionRepository.findByUserId(userId);
    }
}
