-- ========================================
-- 부하 테스트용 데이터베이스 초기화 스크립트
-- ========================================
-- 실행 방법: MySQL Workbench 또는 CLI에서 직접 실행
-- 용도: k6 부하 테스트 전 테스트 데이터 준비
-- ========================================

-- 1. 기존 데이터 삭제 (외래키 체크 비활성화)
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE user_coupons;
TRUNCATE TABLE coupons;
TRUNCATE TABLE point_transactions;
TRUNCATE TABLE points;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE cart_items;
TRUNCATE TABLE products;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;

-- 2. 사용자 생성 (1000명)
INSERT INTO users (name, created_at, updated_at)
SELECT
    CONCAT('TestUser', n),
    NOW(),
    NOW()
FROM (
         SELECT a.n + b.n * 10 + c.n * 100 + d.n * 1000 + 1 AS n
         FROM
             (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a,
             (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b,
             (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c,
             (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) d
     ) numbers
WHERE n <= 5000;



-- 3. 포인트 초기화 (모든 사용자에게 200,000 포인트)
INSERT INTO points (user_id, amount, updated_at)
SELECT id, 200000, NOW() FROM users;

-- 4. 포인트 거래 내역 생성
INSERT INTO point_transactions (point_id, amount, type, balance_after, created_at)
SELECT p.id, 200000, 'CHARGE', 200000, NOW() FROM points p;

-- 5. 상품 생성 (10개, 재고 충분)
INSERT INTO products (name, description, price, stock, category, created_at, updated_at)
VALUES
    ('부하테스트 상품1', '테스트용 인기상품', 10000, 1000, '전자제품', NOW(), NOW()),
    ('부하테스트 상품2', '테스트용 상품', 20000, 800, '의류', NOW(), NOW()),
    ('부하테스트 상품3', '테스트용 상품', 30000, 600, '식품', NOW(), NOW()),
    ('부하테스트 상품4', '테스트용 상품', 40000, 500, '도서', NOW(), NOW()),
    ('부하테스트 상품5', '테스트용 상품', 50000, 400, '가구', NOW(), NOW()),
    ('부하테스트 상품6', '테스트용 상품', 15000, 1000, '전자제품', NOW(), NOW()),
    ('부하테스트 상품7', '테스트용 상품', 25000, 800, '의류', NOW(), NOW()),
    ('부하테스트 상품8', '테스트용 상품', 35000, 600, '식품', NOW(), NOW()),
    ('부하테스트 상품9', '테스트용 상품', 45000, 500, '도서', NOW(), NOW()),
    ('부하테스트 상품10', '테스트용 상품', 55000, 400, '가구', NOW(), NOW());

-- 6. 쿠폰 생성 (3개, 선착순)
INSERT INTO coupons (name, discount_rate, total_quantity, issued_quantity, validity_days, start_date, end_date, created_at, updated_at)
VALUES
    ('부하테스트 쿠폰 20% 1000명', 20, 1000, 0, 30, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW(), NOW());
INSERT INTO coupons (name, discount_rate, total_quantity, issued_quantity, validity_days, start_date, end_date, created_at, updated_at)
VALUES
    ('부하테스트 쿠폰 30% 1000명', 30, 1000, 0, 30, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW(), NOW());
INSERT INTO coupons (name, discount_rate, total_quantity, issued_quantity, validity_days, start_date, end_date, created_at, updated_at)
VALUES
    ('시나리오3 복합트래픽 쿠폰 15% 500명', 15, 500, 0, 30, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW(), NOW());

-- 7. 장바구니 초기 데이터 생성 (주문 테스트용, 사용자 1-50번에게 상품 1-5번 각 1개씩)
INSERT INTO cart_items (user_id, product_id, quantity, created_at, updated_at)
SELECT
    u.id as user_id,
    p.id as product_id,
    1 as quantity,
    NOW() as created_at,
    NOW() as updated_at
FROM
    (SELECT id FROM users WHERE id <= 50) u
CROSS JOIN
    (SELECT id FROM products WHERE id <= 5) p;

-- 8. 검증 쿼리
SELECT '사용자 수' as 항목, COUNT(*) as 개수 FROM users
UNION ALL
SELECT '상품 수', COUNT(*) FROM products
UNION ALL
SELECT '쿠폰 수', COUNT(*) FROM coupons
UNION ALL
SELECT '포인트 계정', COUNT(*) FROM points
UNION ALL
SELECT '장바구니 아이템', COUNT(*) FROM cart_items;

SELECT
    id,
    name,
    stock,
    price,
    category
FROM products;

SELECT
    id,
    name,
    total_quantity,
    issued_quantity,
    (total_quantity - issued_quantity) as remaining
FROM coupons;

