package com.hhplus.hhplus_ecommerce.payment.application;

import com.hhplus.hhplus_ecommerce.common.event.PaymentCompletedEvent;
import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
import com.hhplus.hhplus_ecommerce.order.domain.OrderItem;
import com.hhplus.hhplus_ecommerce.order.repository.OrderRepository;
import com.hhplus.hhplus_ecommerce.payment.dto.response.PaymentResponse;
import com.hhplus.hhplus_ecommerce.point.application.PointService;
import com.hhplus.hhplus_ecommerce.point.domain.Point;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final OrderRepository orderRepository;
    private final UserCouponRepository userCouponRepository;
    private final ProductRepository productRepository;
    private final PointService pointService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void executePayment(Long userId, Long orderId) {
        //todo : 삭제 예정
    }
    private void validatePayment(Order order, Long userId) {
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.canPay()) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_PAY);
        }

        Point point = pointService.getPoint(userId);
        if (!point.hasSufficientBalance(order.getFinalAmount())) {
            throw new BusinessException(ErrorCode.POINT_INSUFFICIENT_BALANCE);
        }

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            if (!product.hasSufficientStock(item.getQuantity())) {
                throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
            }
        }
    }

    @Transactional
    @CacheEvict(value = "userCoupons", allEntries = true)
    public PaymentResponse executePaymentWithResponse(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        validatePayment(order, userId);

        pointService.usePointWithDistributedLock(userId, order.getFinalAmount());

        if (order.getCouponId() != null) {
            UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, order.getCouponId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
            userCoupon.use();
            userCouponRepository.save(userCoupon);
        }

        order.complete();
        orderRepository.save(order);

        // 결제 완료 이벤트 발행
        publishPaymentCompletedEvent(order);

        return new PaymentResponse(
                orderId,
                userId,
                order.getTotalAmount(),
                order.getFinalAmount(),
                LocalDateTime.now()
        );
    }

    /**
     * 결제 완료 이벤트 발행
     * - 데이터 플랫폼 전송
     * - 인기 상품 집계
     */
    private void publishPaymentCompletedEvent(Order order) {
        // 상품 정보를 포함한 주문 아이템 리스트 생성
        var orderItems = order.getItems().stream()
                .map(item -> {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
                    return new PaymentCompletedEvent.OrderItemInfo(
                            item.getProductId(),
                            product.getName(),
                            item.getQuantity(),
                            item.getUnitPrice()
                    );
                })
                .collect(Collectors.toList());

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getCouponId(),
                orderItems,
                LocalDateTime.now()
        );

        eventPublisher.publishEvent(event);
    }

}

