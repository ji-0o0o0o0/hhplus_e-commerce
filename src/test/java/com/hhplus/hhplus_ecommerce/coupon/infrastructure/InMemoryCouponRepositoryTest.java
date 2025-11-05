package com.hhplus.hhplus_ecommerce.coupon.infrastructure;

import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class InMemoryCouponRepositoryTest {

    private InMemoryCouponRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCouponRepository();
    }

    @Test
    @DisplayName("새로운 쿠폰을 저장할 수 있다")
    void save_신규_성공() {
        // given
        Coupon coupon = Coupon.create(
                "10% 할인",
                10,
                100,
                7,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );

        // when
        Coupon saved = repository.save(coupon);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("10% 할인");
        assertThat(saved.getDiscountRate()).isEqualTo(10);
    }

    @Test
    @DisplayName("기존 쿠폰을 업데이트할 수 있다")
    void save_업데이트_성공() {
        // given
        Coupon coupon = Coupon.create(
                "10% 할인",
                10,
                100,
                7,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );
        Coupon saved = repository.save(coupon);

        // when
        Coupon updated = Coupon.builder()
                .id(saved.getId())
                .name(saved.getName())
                .discountRate(saved.getDiscountRate())
                .totalQuantity(saved.getTotalQuantity())
                .issuedQuantity(10)  // 발급 수량 증가
                .validityDays(saved.getValidityDays())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .createdAt(saved.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        Coupon result = repository.save(updated);

        // then
        assertThat(result.getIssuedQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("ID로 쿠폰을 조회할 수 있다")
    void findById_성공() {
        // given
        Coupon coupon = Coupon.create(
                "10% 할인",
                10,
                100,
                7,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        );
        Coupon saved = repository.save(coupon);

        // when
        Optional<Coupon> found = repository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("10% 할인");
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰은 조회되지 않는다")
    void findById_없음() {
        // when
        Optional<Coupon> found = repository.findById(999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("모든 쿠폰을 조회할 수 있다")
    void findAll_성공() {
        // given
        repository.save(Coupon.create("10% 할인", 10, 100, 7,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30)));
        repository.save(Coupon.create("20% 할인", 20, 50, 7,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30)));

        // when
        List<Coupon> coupons = repository.findAll();

        // then
        assertThat(coupons).hasSize(2);
    }

    @Test
    @DisplayName("발급 가능한 쿠폰 목록을 조회할 수 있다")
    void findAvailableCoupons_성공() {
        // given
        repository.save(Coupon.create("10% 할인", 10, 100, 7,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30)));
        repository.save(Coupon.create("만료된 쿠폰", 20, 50, 7,
                LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1)));

        // when
        List<Coupon> availableCoupons = repository.findAvailableCoupons();

        // then
        assertThat(availableCoupons).hasSize(1);
        assertThat(availableCoupons.get(0).getName()).isEqualTo("10% 할인");
    }

    @Test
    @DisplayName("저장소를 초기화할 수 있다")
    void clear_성공() {
        // given
        repository.save(Coupon.create("10% 할인", 10, 100, 7,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30)));
        repository.save(Coupon.create("20% 할인", 20, 50, 7,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30)));

        // when
        repository.clear();

        // then
        assertThat(repository.findAll()).isEmpty();
    }
}