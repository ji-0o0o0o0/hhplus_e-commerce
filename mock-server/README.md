# 항해플러스 E-Commerce Mock API Server

재고 및 쿠폰 관리를 위한 Mock API 서버입니다.

## 설치 및 실행

```bash
# 의존성 설치
npm install

# Mock 서버 실행 (포트: 3000)
npm start

# 지연 시뮬레이션 모드 (1초 지연)
npm run start:delay
```

## API 엔드포인트

서버 실행 후 `http://localhost:3000`에서 사용 가능합니다.

### 상품 API

- `GET /products` - 상품 목록 조회
- `GET /products/:id` - 상품 상세 조회
- `GET /products?category=전자기기` - 카테고리별 조회

**예시:**
```bash
curl http://localhost:3000/products
curl http://localhost:3000/products/1
```

### 쿠폰 API

- `GET /coupons` - 쿠폰 목록 조회
- `GET /coupons/:id` - 쿠폰 상세 조회
- `GET /coupons?status=ACTIVE` - 상태별 조회

**예시:**
```bash
curl http://localhost:3000/coupons
curl http://localhost:3000/coupons?status=ACTIVE
```

### 사용자 쿠폰 API

- `GET /user_coupons?userId=1` - 사용자별 발급된 쿠폰 조회
- `GET /user_coupons?userId=1&status=AVAILABLE` - 사용 가능한 쿠폰만 조회

**예시:**
```bash
curl http://localhost:3000/user_coupons?userId=1
```

### 재고 이력 API

- `GET /inventory_history` - 전체 재고 이력 조회
- `GET /inventory_history?productId=1` - 특정 상품 재고 이력

**예시:**
```bash
curl http://localhost:3000/inventory_history?productId=1
```

## 에러 테스트

특정 ID를 사용하여 에러 상황을 시뮬레이션할 수 있습니다.

| ID | 타입 | 에러 상황 | 설명 |
|---|---|---|---|
| `999` | 상품 | 재고 부족 | stock이 -1로 설정됨 |
| `9999` | 쿠폰 | 쿠폰 소진 | status가 SOLDOUT, 발급 가능 수량 0 |
| `4` | 상품 | 재고 없음 | stock이 0 |

**예시:**
```bash
# 재고 부족 상품 조회
curl http://localhost:3000/products/999

# 소진된 쿠폰 조회
curl http://localhost:3000/coupons/9999
```

## Mock 데이터 구조

### 상품 (products)
```json
{
  "id": 1,
  "name": "Macbook Pro 16",
  "price": 2000000,
  "stock": 10,
  "category": "전자기기",
  "description": "Apple M3 Max 칩 탑재"
}
```

### 쿠폰 (coupons)
```json
{
  "id": 1,
  "name": "신규가입 20% 할인",
  "discountType": "RATE",
  "discountRate": 20,
  "totalQuantity": 100,
  "issuedQuantity": 45,
  "expiryDays": 30,
  "status": "ACTIVE"
}
```

### 사용자 쿠폰 (user_coupons)
```json
{
  "id": 1,
  "userId": 1,
  "couponId": 1,
  "couponName": "신규가입 20% 할인",
  "discountRate": 20,
  "status": "AVAILABLE",
  "issuedAt": "2025-10-01T10:00:00",
  "expiresAt": "2025-11-01T10:00:00"
}
```

## 주요 기능

- ✅ 상품 재고 관리 (stock 필드)
- ✅ 쿠폰 발급 수량 관리 (totalQuantity, issuedQuantity)
- ✅ 쿠폰 상태 관리 (ACTIVE, SOLDOUT)
- ✅ 재고 변동 이력 추적
- ✅ 에러 시나리오 테스트 (특정 ID 사용)

## 기술 스택

- json-server: REST API Mock 서버
- Node.js: 런타임 환경