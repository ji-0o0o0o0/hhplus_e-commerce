-- V1__Create_Initial_Schema.sql
-- E-Commerce Database Initial Schema
-- Created for STEP 07: MySQL Migration
--
-- 설계 원칙:
-- 1. FK 제약조건 사용 안 함 (성능, 데드락 방지, 샤딩 대비)
-- 2. 애플리케이션 레벨에서 참조 무결성 관리
-- 3. 인덱스는 유지 (조회 성능을 위해)
-- 4. 낙관적 락용 version 컬럼 (PRODUCT, COUPON, POINT)

-- =============================================================================
-- 1. USER (사용자)
-- =============================================================================
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '사용자 ID',
    name VARCHAR(100) NOT NULL COMMENT '사용자 이름',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자';

-- =============================================================================
-- 2. POINT (잔액)
-- =============================================================================
CREATE TABLE points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '잔액 ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT '사용자 ID',
    amount BIGINT NOT NULL DEFAULT 0 COMMENT '잔액',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 업데이트 시간',

    CONSTRAINT chk_points_amount CHECK (amount >= 0),

    INDEX idx_points_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='포인트(잔액)';

-- =============================================================================
-- 3. POINT_TRANSACTION (잔액 거래 이력)
-- =============================================================================
CREATE TABLE point_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '거래 ID',
    point_id BIGINT NOT NULL COMMENT '잔액 ID',
    type VARCHAR(10) NOT NULL COMMENT '거래 타입 (CHARGE, USE)',
    amount BIGINT NOT NULL COMMENT '거래 금액',
    balance_after BIGINT NOT NULL COMMENT '거래 후 잔액',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '거래 시간',

    INDEX idx_point_trans_point_created (point_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='포인트 거래 이력';

-- =============================================================================
-- 4. PRODUCT (상품)
-- =============================================================================
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '상품 ID',
    name VARCHAR(200) NOT NULL COMMENT '상품명',
    description TEXT COMMENT '상품 설명',
    price BIGINT NOT NULL COMMENT '가격 (원 단위)',
    stock INT NOT NULL DEFAULT 0 COMMENT '재고 수량',
    category VARCHAR(100) COMMENT '카테고리',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',

    CONSTRAINT chk_products_price CHECK (price >= 0),
    CONSTRAINT chk_products_stock CHECK (stock >= 0),

    INDEX idx_products_category (category),
    INDEX idx_products_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='상품';

-- =============================================================================
-- 5. CART_ITEM (장바구니)
-- =============================================================================
CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '장바구니 항목 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    quantity INT NOT NULL COMMENT '수량',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간'

    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0),

    INDEX idx_cart_items_user (user_id),
    INDEX idx_cart_items_product (product_id),
    UNIQUE INDEX uniq_cart_items_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장바구니';

-- =============================================================================
-- 6. COUPON (쿠폰)
-- =============================================================================
CREATE TABLE coupons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '쿠폰 ID',
    name VARCHAR(100) NOT NULL COMMENT '쿠폰명',
    discount_rate INT NOT NULL COMMENT '할인율 (%)',
    total_quantity INT NOT NULL COMMENT '총 발급 가능 수량',
    issued_quantity INT NOT NULL DEFAULT 0 COMMENT '현재 발급된 수량',
    start_date DATETIME NOT NULL COMMENT '유효 시작일',
    end_date DATETIME NOT NULL COMMENT '유효 종료일',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',

    CONSTRAINT chk_coupons_discount_rate CHECK (discount_rate BETWEEN 1 AND 100),
    CONSTRAINT chk_coupons_issued_quantity CHECK (issued_quantity <= total_quantity),
    CONSTRAINT chk_coupons_dates CHECK (end_date > start_date),

    INDEX idx_coupons_dates (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='쿠폰';

-- =============================================================================
-- 7. USER_COUPON (사용자 쿠폰)
-- =============================================================================
CREATE TABLE user_coupons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '사용자 쿠폰 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    coupon_id BIGINT NOT NULL COMMENT '쿠폰 ID',
    name VARCHAR(100) NOT NULL COMMENT '쿠폰명',
    discount_rate INT NOT NULL COMMENT '할인율 (%)',
    status VARCHAR(20) NOT NULL COMMENT '상태 (AVAILABLE, USED, EXPIRED)',
    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급 시간',
    used_at DATETIME COMMENT '사용 시간',
    expires_at DATETIME NOT NULL COMMENT '만료 시간',

    INDEX idx_user_coupons_user_status (user_id, status),
    INDEX idx_user_coupons_coupon (coupon_id),
    INDEX idx_user_coupons_expires (expires_at),
    UNIQUE INDEX uniq_user_coupons_user_coupon (user_id, coupon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 쿠폰';

-- =============================================================================
-- 8. ORDER (주문)
-- =============================================================================
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '주문 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    coupon_id BIGINT COMMENT '사용한 쿠폰 ID',
    total_amount BIGINT NOT NULL COMMENT '총 금액 (원 단위)',
    discount_amount BIGINT NOT NULL DEFAULT 0 COMMENT '할인 금액 (원 단위)',
    final_amount BIGINT NOT NULL COMMENT '최종 결제 금액 (원 단위)',
    status VARCHAR(20) NOT NULL COMMENT '주문 상태 (PENDING, COMPLETED, CANCELLED)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '주문 생성 시간',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',

    INDEX idx_orders_user_status (user_id, status),
    INDEX idx_orders_coupon (coupon_id),
    INDEX idx_orders_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문';

-- =============================================================================
-- 9. ORDER_ITEM (주문 상품)
-- =============================================================================
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '주문 항목 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    product_name VARCHAR(200) NOT NULL COMMENT '상품명 (스냅샷)',
    unit_price BIGINT NOT NULL COMMENT '단가 (원 단위, 스냅샷)',
    quantity INT NOT NULL COMMENT '수량',
    subtotal INT NOT NULL COMMENT '소계 (원 단위)',

    INDEX idx_order_items_order (order_id),
    INDEX idx_order_items_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문 상품';

-- =============================================================================
-- 10. DATA_TRANSMISSION (외부 데이터 전송 - Outbox)
-- =============================================================================
CREATE TABLE data_transmissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '전송 ID',
    order_id BIGINT NOT NULL COMMENT '주문 ID',
    payload TEXT NOT NULL COMMENT '전송할 데이터 (JSON)',
    status VARCHAR(20) NOT NULL COMMENT '전송 상태 (PENDING, SUCCESS, FAILED)',
    attempts INT NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    error_message TEXT COMMENT '실패 사유',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
    sent_at DATETIME COMMENT '전송 완료 시간',
    last_error_at DATETIME COMMENT '마지막 실패 시간'

    CONSTRAINT chk_data_trans_attempts CHECK (attempts >= 0),

    INDEX idx_data_trans_order (order_id),
    INDEX idx_data_trans_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='외부 데이터 전송 (Outbox 패턴)';

-- =============================================================================
-- 11. PRODUCT_STATISTICS (상품 통계)
-- =============================================================================
CREATE TABLE product_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '통계 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    stats_date DATE NOT NULL COMMENT '통계 기준 날짜',
    sales_count INT NOT NULL DEFAULT 0 COMMENT '판매 수량 (일별 합계)',
    revenue BIGINT NOT NULL DEFAULT 0 COMMENT '매출액 (일별 합계, 원 단위)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',

    UNIQUE INDEX uniq_product_stats_product_date (product_id, stats_date),
    INDEX idx_product_stats_date_sales (stats_date, sales_count DESC),
    INDEX idx_product_stats_date_revenue (stats_date, revenue DESC),
    INDEX idx_product_stats_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='상품 통계';