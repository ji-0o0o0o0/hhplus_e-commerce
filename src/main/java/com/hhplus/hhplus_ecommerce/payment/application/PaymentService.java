package com.hhplus.hhplus_ecommerce.payment.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
import com.hhplus.hhplus_ecommerce.order.domain.OrderItem;
import com.hhplus.hhplus_ecommerce.order.repository.OrderRepository;
import com.hhplus.hhplus_ecommerce.point.TransactionType;
import com.hhplus.hhplus_ecommerce.point.domain.Point;
import com.hhplus.hhplus_ecommerce.point.domain.PointTransaction;
import com.hhplus.hhplus_ecommerce.point.repository.PointRepository;
import com.hhplus.hhplus_ecommerce.point.repository.PointTransactionRepository;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final OrderRepository orderRepository;
    private final PointRepository pointRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserCouponRepository userCouponRepository;
    private final ProductRepository productRepository;

    //결제 실행
    public void executePayment(Long userId, Long orderId) {
        // 1. 주문 조회 및 검증
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        validatePayment(order, userId);

        // 2. 포인트 차감
        Point point = pointRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POINT_NOT_FOUND));
        point.use(order.getFinalAmount());
        Point savedPoint = pointRepository.save(point);

        // 3. 포인트 거래 내역 저장
        PointTransaction transaction = PointTransaction.create(
                userId,
                order.getFinalAmount(),
                TransactionType.USE,
                savedPoint.getAmount()
        );
        pointTransactionRepository.save(transaction);

        // 4. 쿠폰 사용 처리
        if (order.getCouponId() != null) {
            UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, order.getCouponId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
            userCoupon.use();
            userCouponRepository.save(userCoupon);
        }

        // 5. 주문 완료
        order.complete();
        orderRepository.save(order);
    }
    private void validatePayment(Order order, Long userId) {
        // 주문 검증
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.canPay()) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_PAY);
        }

        // 포인트 검증
        Point point = pointRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POINT_NOT_FOUND));
        if (point.getAmount() < order.getFinalAmount()) {
            throw new BusinessException(ErrorCode.POINT_INSUFFICIENT_BALANCE);
        }

        // 재고 검증
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            if (product.getStock() < item.getQuantity()) {
                throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
            }
        }
    }
}

