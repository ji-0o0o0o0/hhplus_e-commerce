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
import com.hhplus.hhplus_ecommerce.order.repository.OrderItemRepository;
import com.hhplus.hhplus_ecommerce.order.repository.OrderRepository;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final UserCouponRepository userCouponRepository;
    private final DistributedLockManager lockManager;
    private final OrderTransactionService  orderTransactionService;
    private final OrderItemRepository orderItemRepository;

    //낙관적락을 통한 주문
    public Order createOrder(Long userId, List<Long> cartItemIds, Long userCouponId) {
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
        Long couponId = null;
        if (userCouponId != null) {
            UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

            if (!userCoupon.isAvailable()) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
            }

            // 주문 총액 계산
            long totalAmount = orderItems.stream()
                    .mapToLong(OrderItem::getSubtotal)
                    .sum();
            discountAmount = userCoupon.calculateDiscount(totalAmount);
            couponId = userCoupon.getCouponId();
        }

        Order order = Order.create(userId, orderItems, couponId, userCouponId, discountAmount);
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
    public Order createOrderWithDistributedLock(Long userId, List<Long> cartItemIds, Long userCouponId) {
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
        Long couponId = null;
        if (userCouponId != null) {
            UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

            if (!userCoupon.isAvailable()) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
            }

            long totalAmount = orderItems.stream()
                    .mapToLong(OrderItem::getSubtotal)
                    .sum();
            discountAmount = userCoupon.calculateDiscount(totalAmount);
            couponId = userCoupon.getCouponId();
        }

        // 주문 생성
        Order order = Order.create(userId, orderItems, couponId, userCouponId, discountAmount);
        orderRepository.save(order);
        orderItems.forEach(item -> item.setOrderId(order.getId()));
        orderItemRepository.saveAll(orderItems);
        return order;
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

    public OrderResponse createOrderWithResponse(Long userId, List<Long> cartItemIds, Long userCouponId) {
        Order order = createOrderWithDistributedLock(userId, cartItemIds, userCouponId);

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

    @Transactional
    public OrderResponse createOrderFromEntireCart(Long userId, Long userCouponId) {

        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }
        List<Long> cartItemIds = cartItems.stream()
                .map(CartItem::getId)
                .toList();

        OrderResponse orderResponse = createOrderWithResponse(userId, cartItemIds, userCouponId);

        cartItemRepository.deleteAllByUserId(userId);

        return orderResponse;

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

    /**
     * 결제 타임아웃된 주문 일괄 취소
     * - PENDING 상태의 주문 중 생성 시간이 timeout 기준 이전인 주문 취소
     * - 재고 자동 복구
     * @param timeoutMinutes 타임아웃 기준 (분)
     * @return 취소된 주문 수
     */
    @Transactional
    public int cancelTimeoutOrders(int timeoutMinutes) {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);

        // PENDING 상태이면서 생성 시간이 timeout 기준 이전인 주문 조회
        List<Order> timeoutOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING,
                timeoutThreshold
        );

        if (timeoutOrders.isEmpty()) {
            return 0;
        }


        log.info("[결제 타임아웃] {} 건의 타임아웃 주문 발견 (타임아웃 기준: {}분)",
                timeoutOrders.size(), timeoutMinutes);

        int canceledCount = 0;
        for (Order order : timeoutOrders) {
            try {
                // 주문 상태를 CANCELLED로 변경
                order.cancel();

                List<OrderItem> items =
                        orderItemRepository.findByOrderId(order.getId());
                // 재고 복구 (낙관적 락 + 재시도)
                for (OrderItem item : items) {
                    increaseProductStock(item.getProductId(), item.getQuantity());
                }

                // 쿠폰 롤백 (사용 전 상태로 복원)
                if (order.getUserCouponId() != null) {
                    UserCoupon userCoupon = userCouponRepository.findById(order.getUserCouponId())
                            .orElse(null);
                    if (userCoupon != null) {
                        userCoupon.rollback();
                        userCouponRepository.save(userCoupon);
                        log.info("[결제 타임아웃] 쿠폰 롤백 완료 - OrderId: {}, UserCouponId: {}",
                                order.getId(), order.getUserCouponId());
                    }
                }

                orderRepository.save(order);
                canceledCount++;

                log.info("[결제 타임아웃] 주문 취소 완료 - OrderId: {}, UserId: {}, 생성시간: {}",
                        order.getId(), order.getUserId(), order.getCreatedAt());

            } catch (Exception e) {
                log.error("[결제 타임아웃] 주문 취소 실패 - OrderId: {}, Error: {}",
                        order.getId(), e.getMessage(), e);
            }
        }

        log.info("[결제 타임아웃] 총 {}건 취소 완료 (처리 대상: {}건)", canceledCount, timeoutOrders.size());
        return canceledCount;
    }

}