package com.hhplus.hhplus_ecommerce.scheduler;

import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
import com.hhplus.hhplus_ecommerce.order.domain.OrderItem;
import com.hhplus.hhplus_ecommerce.order.repository.OrderItemRepository;
import com.hhplus.hhplus_ecommerce.order.repository.OrderRepository;
import com.hhplus.hhplus_ecommerce.product.domain.ProductStatistics;
import com.hhplus.hhplus_ecommerce.product.repository.ProductStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsScheduler {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ProductStatisticsRepository productStatisticsRepository;

    // 매일 자정 1시 실행 - 어제 통계 집계
    @Scheduled(cron = "0 0 1 * * *")
    public void aggregateDailyStatistics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startOfDay = yesterday.atStartOfDay();
        LocalDateTime endOfDay = yesterday.atTime(23, 59, 59);

        log.info("[통계 집계 시작] 날짜: {}", yesterday);

        List<Order> completedOrders = orderRepository
                .findByStatusAndCreatedAtBetween(OrderStatus.COMPLETED, startOfDay, endOfDay);

        if (completedOrders.isEmpty()) {
            log.info("[통계 집계 완료] 완료된 주문 없음");
            return;
        }

        List<Long> orderIds = completedOrders.stream()
                .map(Order::getId)
                .toList();

        List<OrderItem> orderItems = orderItemRepository.findByOrderIdIn(orderIds);

        Map<Long, Integer> salesByProduct = orderItems.stream()
                .collect(Collectors.groupingBy(
                        OrderItem::getProductId,
                        Collectors.summingInt(OrderItem::getQuantity)
                ));

        salesByProduct.forEach((productId, salesCount) -> {
            ProductStatistics stat = new ProductStatistics(
                    null,
                    productId,
                    yesterday,
                    salesCount,
                    calculateRevenue(orderItems, productId)
            );
            productStatisticsRepository.save(stat);
            log.info("[통계 저장] 상품ID={}, 판매량={}", productId, salesCount);
        });

        log.info("[통계 집계 완료] 상품 수={}, 총 주문 수={}", salesByProduct.size(), orderIds.size());
    }



    // 3일 이전 키 삭제
    public void cleanupOldRankingKeys() {
        LocalDate thresholdDate = LocalDate.now().minusDays(4);
        String oldKey = "product:ranking:" + thresholdDate;
        redisTemplate.delete(oldKey);
        log.info("[Redis 키 삭제] 키: {}", oldKey);
    }


    private Long calculateRevenue(List<OrderItem> orderItems, Long productId) {
        return orderItems.stream()
                .filter(item -> item.getProductId().equals(productId))
                .mapToLong(OrderItem::getSubtotal)
                .sum();
    }


}
