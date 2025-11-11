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

    //주문 생성
    public Order createOrder(Long userId, List<Long> cartItemIds, Long couponId) {
        // 1. 장바구니 상품 조회
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

        // 2. 상품 재고 확인 및 주문 항목 생성
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

            // 재고 확인 및 차감 (주문 시 재고 예약) - 동시성 제어 적용
            lockManager.executeWithLock("product:" + product.getId(), () -> {
                if (product.getStock() < cartItem.getQuantity()) {
                    throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
                }
                product.decreaseStock(cartItem.getQuantity());
                productRepository.save(product);
            });

            OrderItem orderItem = OrderItem.create(product, cartItem.getQuantity());
            orderItems.add(orderItem);
        }

        // 3. 쿠폰 확인 및 할인 금액 계산
        Integer discountAmount = 0;
        if (couponId != null) {
            UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

            if (!userCoupon.isAvailable()) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
            }

            // 주문 총액 계산
            int totalAmount = orderItems.stream()
                    .mapToInt(OrderItem::getSubtotal)
                    .sum();
            discountAmount = userCoupon.calculateDiscount(totalAmount);
        }

        // 4. 주문 생성
        Order order = Order.create(userId, orderItems, couponId, discountAmount);
        return orderRepository.save(order);
    }

    // 주문 조회 (단건)
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    //사용자별 주문 목록 조회
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

        // 주문 취소
        order.cancel();

        // 재고 복구 - 동시성 제어 적용
        for (OrderItem item : order.getItems()) {
            lockManager.executeWithLock("product:" + item.getProductId(), () -> {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
                product.increaseStock(item.getQuantity());
                productRepository.save(product);
            });
        }

        orderRepository.save(order);
    }
}