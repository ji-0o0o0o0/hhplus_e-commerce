-- UserCoupon 테이블에 requestId 컬럼 추가
ALTER TABLE user_coupons
    ADD COLUMN request_id VARCHAR(100) COMMENT 'Kafka 비동기 발급 시 요청 ID (멱등성 키)';

-- requestId 인덱스 추가 (상태 조회 API용)
CREATE INDEX idx_user_coupons_request_id ON user_coupons(request_id);

-- Orders 테이블에 userCouponId 컬럼 추가
ALTER TABLE orders
    ADD COLUMN user_coupon_id BIGINT COMMENT '사용한 UserCoupon ID (롤백용)';

-- userCouponId 인덱스 추가
CREATE INDEX idx_orders_user_coupon_id ON orders(user_coupon_id);