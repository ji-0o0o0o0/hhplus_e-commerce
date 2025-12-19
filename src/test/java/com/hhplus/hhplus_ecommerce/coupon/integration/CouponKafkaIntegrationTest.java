package com.hhplus.hhplus_ecommerce.coupon.integration;

import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.application.CouponService;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import com.hhplus.hhplus_ecommerce.integration.TestRedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Kafka 기반 쿠폰 비동기 발급 통합 테스트
 * - Embedded Kafka를 사용한 실제 메시지 발행/소비 테스트
 * - Producer와 Consumer가 정상적으로 동작하는지 검증
 */
@SpringBootTest(
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.data.redis.host=localhost",
                "spring.data.redis.port=16379"
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@EmbeddedKafka(
        partitions = 1,
        topics = {"coupon-issue-request"}
)
@Import(TestRedisConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CouponKafkaIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    private Coupon testCoupon;
    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 100L;

        // 테스트용 쿠폰 생성
        testCoupon = couponService.createCoupon(
                "Kafka 테스트 쿠폰",
                20,
                100,
                30,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
    }

    @Test
    @DisplayName("Kafka를 통한 비동기 쿠폰 발급이 정상 동작한다")
    void issueCouponAsync_성공() {
        // given
        Long couponId = testCoupon.getId();

        // when
        String requestId = couponService.issueCouponAsync(userId, couponId);

        // then
        assertThat(requestId).isNotNull();

        // Consumer가 메시지를 소비하고 쿠폰을 발급할 때까지 대기 (최대 10초)
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Optional<UserCoupon> userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId);
                    assertThat(userCoupon).isPresent();
                    assertThat(userCoupon.get().getStatus()).isEqualTo(CouponStatus.AVAILABLE);
                });

        // 쿠폰 재고 차감 확인
        Coupon updatedCoupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("동일한 사용자가 같은 쿠폰을 중복 요청하면 한 번만 발급된다")
    void issueCouponAsync_중복요청_멱등성보장() {
        // given
        Long couponId = testCoupon.getId();

        // when - 같은 사용자가 동시에 3번 요청
        String requestId1 = couponService.issueCouponAsync(userId, couponId);
        String requestId2 = couponService.issueCouponAsync(userId, couponId);
        String requestId3 = couponService.issueCouponAsync(userId, couponId);

        // then - requestId는 각각 다름
        assertThat(requestId1).isNotEqualTo(requestId2);
        assertThat(requestId2).isNotEqualTo(requestId3);

        // Consumer 처리 대기
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // 중복 발급 방지로 인해 1개만 발급되어야 함
                    Optional<UserCoupon> userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId);
                    assertThat(userCoupon).isPresent();
                });

        // 최종 검증: 쿠폰은 1개만 발급됨
        Coupon updatedCoupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("다수의 사용자가 동시에 쿠폰을 요청하면 모두 발급된다")
    void issueCouponAsync_다수사용자_동시요청() {
        // given
        Long couponId = testCoupon.getId();
        Long[] userIds = {101L, 102L, 103L, 104L, 105L};

        // when - 5명의 사용자가 동시 요청
        for (Long uid : userIds) {
            couponService.issueCouponAsync(uid, couponId);
        }

        // then - 모든 사용자에게 쿠폰이 발급될 때까지 대기
        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    for (Long uid : userIds) {
                        Optional<UserCoupon> userCoupon = userCouponRepository.findByUserIdAndCouponId(uid, couponId);
                        assertThat(userCoupon).isPresent();
                    }
                });

        // 재고 확인: 5개 차감
        Coupon updatedCoupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("재고가 부족한 쿠폰에 대한 발급 요청은 Consumer에서 처리되지 않는다")
    void issueCouponAsync_재고부족_Consumer처리실패() {
        // given - 재고 1개인 쿠폰
        Coupon limitedCoupon = couponService.createCoupon(
                "재고 제한 쿠폰",
                30,
                1,  // 총 수량 1개
                30,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        Long couponId = limitedCoupon.getId();

        // when - 2명이 동시 요청
        couponService.issueCouponAsync(201L, couponId);
        couponService.issueCouponAsync(202L, couponId);

        // then - 첫 번째 요청만 성공
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Coupon updatedCoupon = couponRepository.findById(couponId).orElseThrow();
                    assertThat(updatedCoupon.getIssuedQuantity()).isEqualTo(1);
                });

        // 한 명에게만 발급됨
        boolean user201HasCoupon = userCouponRepository.findByUserIdAndCouponId(201L, couponId).isPresent();
        boolean user202HasCoupon = userCouponRepository.findByUserIdAndCouponId(202L, couponId).isPresent();

        // 둘 중 정확히 한 명만 쿠폰을 받아야 함
        assertThat(user201HasCoupon || user202HasCoupon).isTrue();
        assertThat(user201HasCoupon && user202HasCoupon).isFalse();
    }
}