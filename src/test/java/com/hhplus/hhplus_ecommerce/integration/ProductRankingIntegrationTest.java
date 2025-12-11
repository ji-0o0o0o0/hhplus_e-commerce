package com.hhplus.hhplus_ecommerce.integration;

import com.hhplus.hhplus_ecommerce.product.repository.ProductRankingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Import(TestRedisConfig.class)
@DisplayName("상품 랭킹 통합 테스트")
class ProductRankingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProductRankingRepository productRankingRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String RANKING_KEY = "product:ranking";

    @AfterEach
    void tearDown() {
        // 각 테스트 후 Redis 랭킹 데이터 정리
        redisTemplate.delete(RANKING_KEY);
    }

    @Test
    @DisplayName("상품 판매 수량 증가 - incrementSales")
    void incrementSales_ShouldIncreaseProductSales() {
        // given
        Long productId = 1L;
        int quantity = 10;

        // when
        productRankingRepository.incrementSales(productId, quantity);

        // then
        Long salesCount = productRankingRepository.getSalesCount(productId);
        assertThat(salesCount).isEqualTo(10L);
    }

    @Test
    @DisplayName("상품 판매 수량 여러 번 증가")
    void incrementSales_MultipleTimes_ShouldAccumulate() {
        // given
        Long productId = 1L;

        // when
        productRankingRepository.incrementSales(productId, 5);
        productRankingRepository.incrementSales(productId, 3);
        productRankingRepository.incrementSales(productId, 7);

        // then
        Long salesCount = productRankingRepository.getSalesCount(productId);
        assertThat(salesCount).isEqualTo(15L);
    }

    @Test
    @DisplayName("Top N 상품 조회 - 판매량 순 정렬")
    void getTopProductIds_ShouldReturnTopSellingProducts() {
        // given
        productRankingRepository.incrementSales(1L, 50);
        productRankingRepository.incrementSales(2L, 30);
        productRankingRepository.incrementSales(3L, 100);
        productRankingRepository.incrementSales(4L, 20);
        productRankingRepository.incrementSales(5L, 70);

        // when
        List<Long> topProductIds = productRankingRepository.getTopProductIds(3);

        // then
        assertAll(
                () -> assertThat(topProductIds).hasSize(3),
                () -> {assertThat(topProductIds.get(0)).isEqualTo(3L);},  // 100개
                () -> {assertThat(topProductIds.get(1)).isEqualTo(5L);},  // 70개
                () -> {assertThat(topProductIds.get(2)).isEqualTo(1L);}   // 50개
        );
    }

    @Test
    @DisplayName("Top N 조회 시 상품이 부족한 경우")
    void getTopProductIds_WhenLessThanLimit_ShouldReturnAll() {
        // given
        productRankingRepository.incrementSales(1L, 10);
        productRankingRepository.incrementSales(2L, 20);

        // when
        List<Long> topProductIds = productRankingRepository.getTopProductIds(5);

        // then
        assertThat(topProductIds).hasSize(2);
    }

    @Test
    @DisplayName("상품 랭킹 조회")
    void getRank_ShouldReturnCorrectRank() {
        // given
        productRankingRepository.incrementSales(1L, 50);
        productRankingRepository.incrementSales(2L, 30);
        productRankingRepository.incrementSales(3L, 100);
        productRankingRepository.incrementSales(4L, 20);
        productRankingRepository.incrementSales(5L, 70);

        // when & then
        assertAll(
                () -> assertThat(productRankingRepository.getRank(3L)).isEqualTo(1L),
                () -> assertThat(productRankingRepository.getRank(5L)).isEqualTo(2L),
                () -> assertThat(productRankingRepository.getRank(1L)).isEqualTo(3L),
                () -> assertThat(productRankingRepository.getRank(2L)).isEqualTo(4L),
                () -> assertThat(productRankingRepository.getRank(4L)).isEqualTo(5L)
        );
    }

    @Test
    @DisplayName("존재하지 않는 상품의 랭킹 조회 시 null 반환")
    void getRank_WhenProductNotExists_ShouldReturnNull() {
        // when
        Long rank = productRankingRepository.getRank(999L);

        // then
        assertThat(rank).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 상품의 판매 수량 조회 시 0 반환")
    void getSalesCount_WhenProductNotExists_ShouldReturnZero() {
        // when
        Long salesCount = productRankingRepository.getSalesCount(999L);

        // then
        assertThat(salesCount).isEqualTo(0L);
    }

    @Test
    @DisplayName("Bulk 판매 수량 조회 - RTT 최적화")
    void getSalesCountBulk_ShouldReturnAllSalesCounts() {
        // given
        productRankingRepository.incrementSales(1L, 50);
        productRankingRepository.incrementSales(2L, 30);
        productRankingRepository.incrementSales(3L, 100);
        productRankingRepository.incrementSales(4L, 20);
        productRankingRepository.incrementSales(5L, 70);

        List<Long> productIds = List.of(1L, 2L, 3L, 4L, 5L);

        // when
        Map<Long, Long> salesCountMap = productRankingRepository.getSalesCountBulk(productIds);

        // then
        assertAll(
                () -> assertThat(salesCountMap).hasSize(5),
                () -> assertThat(salesCountMap.get(1L)).isEqualTo(50L),
                () -> assertThat(salesCountMap.get(2L)).isEqualTo(30L),
                () -> assertThat(salesCountMap.get(3L)).isEqualTo(100L),
                () -> assertThat(salesCountMap.get(4L)).isEqualTo(20L),
                () -> assertThat(salesCountMap.get(5L)).isEqualTo(70L)
        );
    }

    @Test
    @DisplayName("Bulk 랭킹 조회 - RTT 최적화")
    void getRankBulk_ShouldReturnAllRanks() {
        // given
        productRankingRepository.incrementSales(1L, 50);
        productRankingRepository.incrementSales(2L, 30);
        productRankingRepository.incrementSales(3L, 100);
        productRankingRepository.incrementSales(4L, 20);
        productRankingRepository.incrementSales(5L, 70);

        List<Long> productIds = List.of(1L, 2L, 3L, 4L, 5L);

        // when
        Map<Long, Long> rankMap = productRankingRepository.getRankBulk(productIds);

        // then
        assertAll(
                () -> assertThat(rankMap).hasSize(5),
                () -> assertThat(rankMap.get(3L)).isEqualTo(1L),  
                () -> assertThat(rankMap.get(5L)).isEqualTo(2L),  
                () -> assertThat(rankMap.get(1L)).isEqualTo(3L),  
                () -> assertThat(rankMap.get(2L)).isEqualTo(4L),  
                () -> assertThat(rankMap.get(4L)).isEqualTo(5L)   
        );
    }

    @Test
    @DisplayName("Bulk 조회 시 존재하지 않는 상품 포함")
    void getBulk_WithNonExistentProducts_ShouldReturnZeroOrNull() {
        // given
        productRankingRepository.incrementSales(1L, 50);
        productRankingRepository.incrementSales(2L, 30);

        List<Long> productIds = List.of(1L, 2L, 999L);

        // when
        Map<Long, Long> salesCountMap = productRankingRepository.getSalesCountBulk(productIds);
        Map<Long, Long> rankMap = productRankingRepository.getRankBulk(productIds);

        // then
        assertThat(salesCountMap.get(999L)).isEqualTo(0L);  // 존재하지 않으면 0
        assertThat(rankMap.get(999L)).isNull();             // 존재하지 않으면 null
    }

    @Test
    @DisplayName("동일 판매량인 경우 랭킹 처리")
    void getRank_WithSameSalesCount_ShouldHandleCorrectly() {
        // given
        productRankingRepository.incrementSales(1L, 50);
        productRankingRepository.incrementSales(2L, 50);
        productRankingRepository.incrementSales(3L, 50);

        // when
        Long rank1 = productRankingRepository.getRank(1L);
        Long rank2 = productRankingRepository.getRank(2L);
        Long rank3 = productRankingRepository.getRank(3L);

        // then
        // 동일 점수인 경우 Redis Sorted Set은 member의 사전순으로 정렬
        // 모두 1-3등 사이에 있어야 함
        assertThat(rank1).isBetween(1L, 3L);
        assertThat(rank2).isBetween(1L, 3L);
        assertThat(rank3).isBetween(1L, 3L);
    }
}
