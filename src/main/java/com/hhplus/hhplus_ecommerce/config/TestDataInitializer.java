package com.hhplus.hhplus_ecommerce.config;

import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.point.domain.Point;
import com.hhplus.hhplus_ecommerce.point.repository.PointRepository;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import com.hhplus.hhplus_ecommerce.user.domain.User;
import com.hhplus.hhplus_ecommerce.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TestDataInitializer {
    private final UserRepository userRepository;
    private final PointRepository pointRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;

    @PostConstruct
    public void init() {
        initUsers();
        initProducts();
        initCoupons();
    }

    //테스트 유저 생성
    private void initUsers() {
        User user1 = userRepository.save(User.create("김철수"));
        User user2 = userRepository.save(User.create("이영희"));
        User user3 = userRepository.save(User.create("박민수"));

        // 각 유저의 포인트 0원으로 초기화
        pointRepository.save(Point.create(user1.getId()));
        pointRepository.save(Point.create(user2.getId()));
        pointRepository.save(Point.create(user3.getId()));

        System.out.println("✅ 테스트 유저 생성 완료: 김철수(ID:1), 이영희(ID:2), 박민수(ID:3)");
    }

    //테스트 상품 생성
    private void initProducts() {
        productRepository.save(Product.create(
                "맥북 프로 16인치",
                "M3 Max 칩, 36GB RAM, 1TB SSD",
                4500000,
                10,
                "전자제품"
        ));

        productRepository.save(Product.create(
                "아이폰 15 Pro",
                "256GB, 티타늄 화이트",
                1550000,
                20,
                "전자제품"
        ));

        productRepository.save(Product.create(
                "에어팟 프로 2세대",
                "노이즈 캔슬링, USB-C",
                350000,
                50,
                "전자제품"
        ));

        productRepository.save(Product.create(
                "매직 키보드",
                "한글 각인, 스페이스 그레이",
                150000,
                30,
                "액세서리"
        ));

        productRepository.save(Product.create(
                "매직 마우스",
                "멀티터치 표면, 충전식",
                99000,
                40,
                "액세서리"
        ));

        System.out.println("✅ 테스트 상품 5개 생성 완료");
    }

    // 쿠폰 생성
    private void initCoupons() {
        // 1. 선착순 10% 할인 쿠폰 (100개 한정, 발급 후 7일간 유효)
        couponRepository.save(Coupon.create(
                "신규가입 10% 할인",
                10,
                100,
                7,  
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        ));

        // 2. 선착순 20% 할인 쿠폰 (50개 한정, 발급 후 14일간 유효)
        couponRepository.save(Coupon.create(
                "VIP 회원 20% 할인",
                20,
                50,
                14,  
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30)
        ));

        // 3. 선착순 15% 할인 쿠폰 (200개 한정, 발급 후 30일간 유효)
        couponRepository.save(Coupon.create(
                "첫 구매 15% 할인",
                15,
                200,
                30,  
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(60)
        ));

        System.out.println("✅ 테스트 쿠폰 3개 생성 완료");
    }
}