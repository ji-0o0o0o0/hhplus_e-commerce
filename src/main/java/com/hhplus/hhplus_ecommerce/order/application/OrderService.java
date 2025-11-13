package com.hhplus.hhplus_ecommerce.order.application;

import com.hhplus.hhplus_ecommerce.cart.domain.CartItem;
import com.hhplus.hhplus_ecommerce.cart.repository.CartItemRepository;
import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.common.lock.LockManager;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
import com.hhplus.hhplus_ecommerce.order.domain.OrderItem;
import com.hhplus.hhplus_ecommerce.order.repository.OrderRepository;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final UserCouponRepository userCouponRepository;
    private final LockManager lockManager;

    public Order createOrder(Long userId, List<Long> cartItemIds, Long couponId) {
        // 장바구니 항목 조회 및 검증
        List<CartItem> cartItems = new ArrayList<>();
        for (Long cartItemId : cartItemIds) {
            CartItem cartItem = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

            if (!userId.equals(cartItem.getUserId())) {
                throw new BusinessException(ErrorCode.CART_ITEM_ACCESS_DENIED);
            }
            cartItems.add(cartItem);
        }

        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_EMPTY_ITEMS);
        }

        // 재고 차감 (낙관적 락 + 재시도)
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            int maxRetries = 5;
            int retryCount = 0;
            boolean success = false;

            while (retryCount < maxRetries && !success) {
                try {
                    Product product = productRepository.findById(cartItem.getProductId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

                    // 재고 확인 및 차감
                    if (!product.hasSufficientStock(cartItem.getQuantity())) {
                        throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
                    }

                    product.decreaseStock(cartItem.getQuantity());
                    productRepository.saveAndFlush(product);  // 즉시 DB 반영

                    OrderItem orderItem = OrderItem.create(product, cartItem.getQuantity());
                    orderItems.add(orderItem);
                    success = true;

                } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                    retryCount++;
                    if (retryCount >= maxRetries) {
                        throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
                    }
                    // 짧은 대기 후 재시도
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(ErrorCode.LOCK_INTERRUPTED);
                    }
                }
            }
        }

        // 쿠폰 할인 계산
        Long discountAmount = 0L;
        if (couponId != null) {
            UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

            if (!userCoupon.isAvailable()) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
            }

            // 주문 총액 계산
            long totalAmount = orderItems.stream()
                    .mapToLong(OrderItem::getSubtotal)
                    .sum();
            discountAmount = userCoupon.calculateDiscount(totalAmount);
        }

        Order order = Order.create(userId, orderItems, couponId, discountAmount);
        return orderRepository.save(order);
    }

    // 주문 조회 (단건)
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    // 주문 상태별 조회
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    // 모든 주문 조회
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    //주문 취소
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.cancel();

        // 재고 복구 (낙관적 락 + 재시도)
        for (OrderItem item : order.getItems()) {
            int maxRetries = 5;
            int retryCount = 0;
            boolean success = false;

            while (retryCount < maxRetries && !success) {
                try {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
                    product.increaseStock(item.getQuantity());
                    productRepository.saveAndFlush(product);
                    success = true;

                } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                    retryCount++;
                    if (retryCount >= maxRetries) {
                        throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL);
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(ErrorCode.LOCK_INTERRUPTED);
                    }
                }
            }
        }

        orderRepository.save(order);
    }
}