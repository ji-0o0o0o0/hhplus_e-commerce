package com.hhplus.hhplus_ecommerce.order.application;

import com.hhplus.hhplus_ecommerce.cart.domain.CartItem;
import com.hhplus.hhplus_ecommerce.cart.repository.CartItemRepository;
import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.common.lock.DistributedLockManager;
import com.hhplus.hhplus_ecommerce.common.lock.RedisLockKey;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
import com.hhplus.hhplus_ecommerce.order.domain.OrderItem;
import com.hhplus.hhplus_ecommerce.order.dto.response.OrderItemDto;
import com.hhplus.hhplus_ecommerce.order.dto.response.OrderListDto;
import com.hhplus.hhplus_ecommerce.order.dto.response.OrderListResponse;
import com.hhplus.hhplus_ecommerce.order.dto.response.OrderResponse;
import com.hhplus.hhplus_ecommerce.order.repository.OrderRepository;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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
    private final DistributedLockManager lockManager;
    private final OrderTransactionService  orderTransactionService;

    //낙관적락을 통한 주문
    public Order createOrder(Long userId, List<Long> cartItemIds, Long couponId) {
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
            Product product = decreaseProductStock(cartItem.getProductId(),cartItem.getQuantity());
            OrderItem orderItem = OrderItem.create(product, cartItem.getQuantity());
            orderItems.add(orderItem);
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

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    //낙관적락을 통한 주문 취소
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.cancel();

        // 재고 복구 (낙관적 락 + 재시도)
        for (OrderItem item : order.getItems()) {
            increaseProductStock(item.getProductId(), item.getQuantity());
        }

        orderRepository.save(order);
    }
    //분산락을 통한 주문
    public Order createOrderWithDistributedLock(Long userId, List<Long> cartItemIds, Long couponId) {
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

        // 재고 차감 (분산락)
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = decreaseProductStockWithDistributedLock(cartItem.getProductId(), cartItem.getQuantity());
            OrderItem orderItem = OrderItem.create(product, cartItem.getQuantity());
            orderItems.add(orderItem);
        }

        Long discountAmount = 0L;
        if (couponId != null) {
            UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

            if (!userCoupon.isAvailable()) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
            }

            long totalAmount = orderItems.stream()
                    .mapToLong(OrderItem::getSubtotal)
                    .sum();
            discountAmount = userCoupon.calculateDiscount(totalAmount);
        }

        // 4. 주문 생성 (83-84번째 줄과 동일)
        Order order = Order.create(userId, orderItems, couponId, discountAmount);
        return orderRepository.save(order);
    }

    //분산락 통한 주문 취소
    public void cancelOrderWithDistributedLock(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.cancel();

        // 재고 복구 (분산락)
        for (OrderItem item : order.getItems()) {
            increaseProductStockWithDistributedLock(item.getProductId(), item.getQuantity());
        }

        orderRepository.save(order);
    }

    //낙관적락(성능 비교용)
    @Retryable(
            retryFor = {ObjectOptimisticLockingFailureException.class, OptimisticLockException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 50, maxDelay = 200)
    )
    private Product decreaseProductStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.hasSufficientStock(quantity)) {
            throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
        }
        product.decreaseStock(quantity);
        return  productRepository.saveAndFlush(product);
    }

    @Retryable(
            retryFor = {ObjectOptimisticLockingFailureException.class, OptimisticLockException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 50, maxDelay = 200)
    )
    private void increaseProductStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.increaseStock(quantity);
        productRepository.saveAndFlush(product);
    }

    //분산락
    private Product decreaseProductStockWithDistributedLock(Long productId, int quantity) {
        String lockKey = RedisLockKey.productStock(productId);

        return lockManager.executeWithLock(lockKey, 5L, 10L, () ->
                orderTransactionService.decreaseProductStockTransaction(productId, quantity)
        );
    }



    private void increaseProductStockWithDistributedLock(Long productId, int quantity) {
        String lockKey = RedisLockKey.productStock(productId);
        lockManager.executeWithLock(lockKey, 5L, 10L, () ->
                orderTransactionService.increaseProductStockTransaction(productId, quantity)
        );
    }

    public OrderResponse createOrderWithResponse(Long userId, List<Long> cartItemIds, Long couponId) {
        Order order = createOrderWithDistributedLock(userId, cartItemIds, couponId);

        List<OrderItemDto> orderItemDtos = convertToOrderItemDtos(order);

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getStatus(),
                orderItemDtos,
                order.getCreatedAt()
        );
    }

    public OrderResponse createOrderFromEntireCart(Long userId, Long couponId) {

        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }
        List<Long> cartItemIds = cartItems.stream()
                .map(CartItem::getId)
                .toList();

        return createOrderWithResponse(userId, cartItemIds, couponId);

    }

    public OrderListResponse getOrdersByUserIdWithResponse(Long userId, Integer page, Integer size) {
        List<Order> orders = getOrdersByUserId(userId);

        List<OrderListDto> orderDtos = orders.stream()
                .map(o -> new OrderListDto(
                        o.getId(),
                        o.getFinalAmount(),
                        o.getStatus(),
                        o.getCreatedAt()
                ))
                .toList();

        return new OrderListResponse(
                orderDtos,
                (long) orders.size(),
                (orders.size() + size - 1) / size,
                page,
                size
        );
    }

    public OrderResponse getOrderDetailResponse(Long orderId) {
        Order order = getOrder(orderId);

        List<OrderItemDto> orderItemDtos = convertToOrderItemDtos(order);

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getStatus(),
                orderItemDtos,
                order.getCreatedAt()
        );
    }
    private List<OrderItemDto> convertToOrderItemDtos(Order order) {
        return order.getItems().stream()
                .map(item -> {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

                    return new OrderItemDto(
                            item.getId(),
                            item.getProductId(),
                            product.getName(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getSubtotal()
                    );
                })
                .toList();
    }


}