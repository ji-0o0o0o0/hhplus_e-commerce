package com.hhplus.hhplus_ecommerce.coupon.infrastructure;

import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryCouponRepository implements CouponRepository {

    private final Map<Long, Coupon> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Coupon save(Coupon coupon) {
        if (coupon.getId() == null) {
            Coupon newCoupon = Coupon.builder()
                    .id(idGenerator.getAndIncrement())
                    .name(coupon.getName())
                    .discountRate(coupon.getDiscountRate())
                    .totalQuantity(coupon.getTotalQuantity())
                    .issuedQuantity(coupon.getIssuedQuantity())
                    .validityDays(coupon.getValidityDays())
                    .startDate(coupon.getStartDate())
                    .endDate(coupon.getEndDate())
                    .build();
            store.put(newCoupon.getId(), newCoupon);
            return newCoupon;
        } else {
            // 기존 쿠폰 업데이트
            store.put(coupon.getId(), coupon);
            return coupon;
        }
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Coupon> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Coupon> findAvailableCoupons() {
        LocalDateTime now = LocalDateTime.now();
        return store.values().stream()
                .filter(coupon -> coupon.canIssue() && coupon.isValid())
               .toList();
    }

    public void clear() {
        store.clear();
        idGenerator.set(1);
    }
}