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

-- 2. 사용자 생성 (100명)
INSERT INTO users (email, name, created_at, updated_at) VALUES
('user1@test.com', 'TestUser1', NOW(), NOW()),
('user2@test.com', 'TestUser2', NOW(), NOW()),
('user3@test.com', 'TestUser3', NOW(), NOW()),
('user4@test.com', 'TestUser4', NOW(), NOW()),
('user5@test.com', 'TestUser5', NOW(), NOW()),
('user6@test.com', 'TestUser6', NOW(), NOW()),
('user7@test.com', 'TestUser7', NOW(), NOW()),
('user8@test.com', 'TestUser8', NOW(), NOW()),
('user9@test.com', 'TestUser9', NOW(), NOW()),
('user10@test.com', 'TestUser10', NOW(), NOW()),
('user11@test.com', 'TestUser11', NOW(), NOW()),
('user12@test.com', 'TestUser12', NOW(), NOW()),
('user13@test.com', 'TestUser13', NOW(), NOW()),
('user14@test.com', 'TestUser14', NOW(), NOW()),
('user15@test.com', 'TestUser15', NOW(), NOW()),
('user16@test.com', 'TestUser16', NOW(), NOW()),
('user17@test.com', 'TestUser17', NOW(), NOW()),
('user18@test.com', 'TestUser18', NOW(), NOW()),
('user19@test.com', 'TestUser19', NOW(), NOW()),
('user20@test.com', 'TestUser20', NOW(), NOW()),
('user21@test.com', 'TestUser21', NOW(), NOW()),
('user22@test.com', 'TestUser22', NOW(), NOW()),
('user23@test.com', 'TestUser23', NOW(), NOW()),
('user24@test.com', 'TestUser24', NOW(), NOW()),
('user25@test.com', 'TestUser25', NOW(), NOW()),
('user26@test.com', 'TestUser26', NOW(), NOW()),
('user27@test.com', 'TestUser27', NOW(), NOW()),
('user28@test.com', 'TestUser28', NOW(), NOW()),
('user29@test.com', 'TestUser29', NOW(), NOW()),
('user30@test.com', 'TestUser30', NOW(), NOW()),
('user31@test.com', 'TestUser31', NOW(), NOW()),
('user32@test.com', 'TestUser32', NOW(), NOW()),
('user33@test.com', 'TestUser33', NOW(), NOW()),
('user34@test.com', 'TestUser34', NOW(), NOW()),
('user35@test.com', 'TestUser35', NOW(), NOW()),
('user36@test.com', 'TestUser36', NOW(), NOW()),
('user37@test.com', 'TestUser37', NOW(), NOW()),
('user38@test.com', 'TestUser38', NOW(), NOW()),
('user39@test.com', 'TestUser39', NOW(), NOW()),
('user40@test.com', 'TestUser40', NOW(), NOW()),
('user41@test.com', 'TestUser41', NOW(), NOW()),
('user42@test.com', 'TestUser42', NOW(), NOW()),
('user43@test.com', 'TestUser43', NOW(), NOW()),
('user44@test.com', 'TestUser44', NOW(), NOW()),
('user45@test.com', 'TestUser45', NOW(), NOW()),
('user46@test.com', 'TestUser46', NOW(), NOW()),
('user47@test.com', 'TestUser47', NOW(), NOW()),
('user48@test.com', 'TestUser48', NOW(), NOW()),
('user49@test.com', 'TestUser49', NOW(), NOW()),
('user50@test.com', 'TestUser50', NOW(), NOW()),
('user51@test.com', 'TestUser51', NOW(), NOW()),
('user52@test.com', 'TestUser52', NOW(), NOW()),
('user53@test.com', 'TestUser53', NOW(), NOW()),
('user54@test.com', 'TestUser54', NOW(), NOW()),
('user55@test.com', 'TestUser55', NOW(), NOW()),
('user56@test.com', 'TestUser56', NOW(), NOW()),
('user57@test.com', 'TestUser57', NOW(), NOW()),
('user58@test.com', 'TestUser58', NOW(), NOW()),
('user59@test.com', 'TestUser59', NOW(), NOW()),
('user60@test.com', 'TestUser60', NOW(), NOW()),
('user61@test.com', 'TestUser61', NOW(), NOW()),
('user62@test.com', 'TestUser62', NOW(), NOW()),
('user63@test.com', 'TestUser63', NOW(), NOW()),
('user64@test.com', 'TestUser64', NOW(), NOW()),
('user65@test.com', 'TestUser65', NOW(), NOW()),
('user66@test.com', 'TestUser66', NOW(), NOW()),
('user67@test.com', 'TestUser67', NOW(), NOW()),
('user68@test.com', 'TestUser68', NOW(), NOW()),
('user69@test.com', 'TestUser69', NOW(), NOW()),
('user70@test.com', 'TestUser70', NOW(), NOW()),
('user71@test.com', 'TestUser71', NOW(), NOW()),
('user72@test.com', 'TestUser72', NOW(), NOW()),
('user73@test.com', 'TestUser73', NOW(), NOW()),
('user74@test.com', 'TestUser74', NOW(), NOW()),
('user75@test.com', 'TestUser75', NOW(), NOW()),
('user76@test.com', 'TestUser76', NOW(), NOW()),
('user77@test.com', 'TestUser77', NOW(), NOW()),
('user78@test.com', 'TestUser78', NOW(), NOW()),
('user79@test.com', 'TestUser79', NOW(), NOW()),
('user80@test.com', 'TestUser80', NOW(), NOW()),
('user81@test.com', 'TestUser81', NOW(), NOW()),
('user82@test.com', 'TestUser82', NOW(), NOW()),
('user83@test.com', 'TestUser83', NOW(), NOW()),
('user84@test.com', 'TestUser84', NOW(), NOW()),
('user85@test.com', 'TestUser85', NOW(), NOW()),
('user86@test.com', 'TestUser86', NOW(), NOW()),
('user87@test.com', 'TestUser87', NOW(), NOW()),
('user88@test.com', 'TestUser88', NOW(), NOW()),
('user89@test.com', 'TestUser89', NOW(), NOW()),
('user90@test.com', 'TestUser90', NOW(), NOW()),
('user91@test.com', 'TestUser91', NOW(), NOW()),
('user92@test.com', 'TestUser92', NOW(), NOW()),
('user93@test.com', 'TestUser93', NOW(), NOW()),
('user94@test.com', 'TestUser94', NOW(), NOW()),
('user95@test.com', 'TestUser95', NOW(), NOW()),
('user96@test.com', 'TestUser96', NOW(), NOW()),
('user97@test.com', 'TestUser97', NOW(), NOW()),
('user98@test.com', 'TestUser98', NOW(), NOW()),
('user99@test.com', 'TestUser99', NOW(), NOW()),
('user100@test.com', 'TestUser100', NOW(), NOW());

-- 3. 포인트 초기화 (모든 사용자에게 200,000 포인트)
INSERT INTO points (user_id, amount, updated_at)
SELECT id, 200000, NOW() FROM users;

-- 4. 포인트 거래 내역 생성
INSERT INTO point_transactions (point_id, amount, type, balance_after, created_at)
SELECT p.id, 200000, 'CHARGE', 200000, NOW() FROM points p;

-- 5. 상품 생성 (10개, 재고 충분)
INSERT INTO products (name, description, price, stock, created_at, updated_at) VALUES
('MacBook Pro 16', '고성능 노트북', 3500000, 1000, NOW(), NOW()),
('iPhone 15 Pro', '최신 스마트폰', 1500000, 1000, NOW(), NOW()),
('AirPods Pro', '노이즈 캔슬링 이어폰', 350000, 2000, NOW(), NOW()),
('iPad Air', '태블릿 PC', 900000, 1500, NOW(), NOW()),
('Apple Watch Ultra', '프리미엄 스마트워치', 1200000, 800, NOW(), NOW()),
('Magic Keyboard', '무선 키보드', 150000, 3000, NOW(), NOW()),
('Magic Mouse', '무선 마우스', 100000, 3000, NOW(), NOW()),
('HomePod mini', '스마트 스피커', 120000, 1500, NOW(), NOW()),
('AirTag 4-pack', '분실 방지 태그', 150000, 2500, NOW(), NOW()),
('USB-C Cable', '충전 케이블', 25000, 5000, NOW(), NOW());

-- 6. 쿠폰 생성 (2개, 선착순 100명)
INSERT INTO coupons (name, discount_rate, total_quantity, remaining_quantity, start_date, end_date, created_at, updated_at) VALUES
('신규가입 쿠폰', 20, 100, 100, '2024-01-01', '2025-12-31', NOW(), NOW()),
('VIP 할인 쿠폰', 30, 100, 100, '2024-01-01', '2025-12-31', NOW(), NOW());

-- 7. 검증 쿼리
SELECT '=== 데이터 초기화 완료 ===' as status;
SELECT COUNT(*) as user_count FROM users;
SELECT COUNT(*) as product_count FROM products;
SELECT COUNT(*) as coupon_count FROM coupons;
SELECT SUM(amount) as total_points FROM points;
SELECT COUNT(*) as point_tx_count FROM point_transactions;
