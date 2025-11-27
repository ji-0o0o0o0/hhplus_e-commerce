# 📦 e-commerce API 명세서

---

## 🔐 인증

> **Note**:
> - 실무에서는 JWT 토큰으로 사용자 인증을 처리합니다.
> - userId는 토큰에서 자동으로 추출되어야 하며, 다른 사용자의 데이터에 접근할 수 없습니다.
> - 본 과제에서는 단순화를 위해 userId를 파라미터로 전달하는 방식을 사용합니다.
> - 모든 API(상품 조회 제외)는 인증된 사용자만 호출 가능합니다.

### 인증이 필요한 API
- 장바구니 관련 API
- 주문 관련 API
- 결제 관련 API
- 쿠폰 관련 API

### 인증이 불필요한 API
- 상품 목록 조회
- 상품 상세 조회
- 인기 상품 조회

---

## 1. 상품 (Product)

### 1.1 상품 목록 조회
`GET /api/products`

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | Number | X | 0 | 페이지 번호 (0부터 시작) |
| size | Number | X | 10 | 페이지 크기 |
| keyword | String | X | - | 상품명 검색어 |

#### Request Example
```
GET /api/products?page=0&size=10&keyword=macbook
```

#### Response

**200 OK**: 성공적으로 조회된 경우

| 필드명 | 데이터 타입 | 설명 |
|:---:|:---:|:---:|
| `code` | Number | HTTP 상태 코드 |
| `message` | String | 요청 처리 메시지 |
| `data` | Object | 응답 데이터 |
| `data.products` | Array | 상품 목록 |
| `products[].id` | Number | 상품 ID |
| `products[].name` | String | 상품 이름 |
| `products[].price` | Number | 상품 가격 |
| `products[].stock` | Number | 상품 재고 |
| `products[].category` | String | 카테고리 |
| `data.totalElements` | Number | 전체 상품 수 |
| `data.totalPages` | Number | 전체 페이지 수 |
| `data.currentPage` | Number | 현재 페이지 |
| `data.size` | Number | 페이지 크기 |

```json
{
  "code": 200,
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "products": [
      {
        "id": 1,
        "name": "Macbook Pro",
        "price": 2000000,
        "stock": 10,
        "category": "전자제품"
      },
      {
        "id": 2,
        "name": "iPhone 12",
        "price": 1200000,
        "stock": 20,
        "category": "전자제품"
      }
    ],
    "totalElements": 100,
    "totalPages": 10,
    "currentPage": 0,
    "size": 10
  }
}
```

#### Error Response

**400 Bad Request**: 잘못된 페이지 요청
```json
{
  "code": 400,
  "message": "잘못된 요청입니다. 페이지 번호는 0 이상이어야 합니다.",
  "data": null
}
```

---

### 1.2 상품 상세 조회
`GET /api/products/{productId}`

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| productId | Number | O | 상품 ID |

#### Response

**200 OK**: 성공적으로 조회된 경우

| 필드명 | 데이터 타입 | 설명 |
|:---:|:---:|:---:|
| `code` | Number | HTTP 상태 코드 |
| `message` | String | 요청 처리 메시지 |
| `data` | Object | 응답 데이터 |
| `data.id` | Number | 상품 ID |
| `data.name` | String | 상품 이름 |
| `data.description` | String | 상품 설명 |
| `data.price` | Number | 상품 가격 |
| `data.stock` | Number | 상품 재고 |
| `data.category` | String | 카테고리 |

```json
{
  "code": 200,
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "id": 1,
    "name": "Macbook Pro",
    "description": "고성능 노트북",
    "price": 2000000,
    "stock": 10,
    "category": "전자제품"
  }
}
```

#### Error Response

**404 Not Found**: 상품을 찾을 수 없음
```json
{
  "code": 404,
  "message": "상품을 찾을 수 없습니다.",
  "data": null
}
```

---

### 1.3 인기 상품 조회
`GET /api/products/popular`

최근 3일간 판매량 기준 상위 5개 상품을 조회합니다.

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| days | Number | X | 3 | 집계 기간 (일) |

#### Response

**200 OK**: 성공적으로 조회된 경우

| 필드명 | 데이터 타입 | 설명 |
|:---:|:---:|:---:|
| `code` | Number | HTTP 상태 코드 |
| `message` | String | 요청 처리 메시지 |
| `data` | Object | 응답 데이터 |
| `data.products` | Array | 인기 상품 목록 (최대 5개) |
| `products[].id` | Number | 상품 ID |
| `products[].name` | String | 상품 이름 |
| `products[].price` | Number | 상품 가격 |
| `products[].category` | String | 카테고리 |
| `products[].salesCount` | Number | 판매 수량 |

```json
{
  "code": 200,
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "products": [
      {
        "id": 1,
        "name": "Macbook Pro",
        "price": 2000000,
        "category": "전자제품",
        "salesCount": 150
      },
      {
        "id": 2,
        "name": "iPhone 12",
        "price": 1200000,
        "category": "전자제품",
        "salesCount": 120
      }
    ]
  }
}
```

#### Error Response

**400 Bad Request**: 잘못된 요청
```json
{
  "code": 400,
  "message": "잘못된 요청입니다.",
  "data": null
}
```

---

## 2. 장바구니 (Cart)

### 2.1 장바구니 추가
`POST /api/carts`

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| userId | Number | O | 사용자 ID |
| productId | Number | O | 상품 ID |
| quantity | Number | O | 수량 |

```json
{
  "userId": 1,
  "productId": 1,
  "quantity": 2
}
```

#### Response

**201 Created**: 장바구니에 성공적으로 추가됨

```json
{
  "code": 201,
  "message": "장바구니에 추가되었습니다.",
  "data": {
    "cartItemId": 1,
    "userId": 1,
    "productId": 1,
    "productName": "Macbook Pro",
    "quantity": 2,
    "price": 2000000
  }
}
```

#### Error Response

| 상태 코드 | 설명 | 응답 예시 |
|----------|------|-----------|
| 400 | 재고 부족 | `{"code": 400, "message": "재고가 부족합니다."}` |
| 404 | 상품 없음 | `{"code": 404, "message": "상품을 찾을 수 없습니다."}` |
| 404 | 사용자 없음 | `{"code": 404, "message": "사용자를 찾을 수 없습니다."}` |

---

### 2.2 장바구니 삭제
`DELETE /api/carts/{cartItemId}`

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| cartItemId | Number | O | 장바구니 항목 ID |

#### Response

**404 Not Found**: 사용자를 찾을 수 없음
```json
{
  "code": 404,
  "message": "사용자를 찾을 수 없습니다.",
  "data": null
}
```

**204 No Content**: 성공적으로 삭제됨

#### Error Response

| 상태 코드 | 설명 | 응답 예시 |
|----------|------|-----------|
| 403 | 권한 없음 | `{"code": 403, "message": "권한이 없습니다."}` |
| 404 | 장바구니 항목 없음 | `{"code": 404, "message": "장바구니 항목을 찾을 수 없습니다."}` |
| 404 | 사용자 없음 | `{"code": 404, "message": "사용자를 찾을 수 없습니다."}` |

---

### 2.3 장바구니 조회
`GET /api/carts`

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| userId | Number | O | 사용자 ID |

#### Response

**200 OK**: 성공적으로 조회됨

```json
{
  "code": 200,
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "cartItems": [
      {
        "cartItemId": 1,
        "productId": 1,
        "productName": "Macbook Pro",
        "quantity": 2,
        "price": 2000000,
        "subtotal": 4000000
      },
      {
        "cartItemId": 2,
        "productId": 2,
        "productName": "iPhone 12",
        "quantity": 1,
        "price": 1200000,
        "subtotal": 1200000
      }
    ],
    "totalAmount": 5200000
  }
}
```

#### Error Response

**400 Bad Request**: 잘못된 요청
```json
{
  "code": 400,
  "message": "잘못된 요청입니다.",
  "data": null
}
```

---

## 3. 주문 (Order)

### 3.1 주문 생성 (선택 주문)
`POST /api/orders`

장바구니에서 선택한 상품들만 주문합니다.

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| userId | Number | O | 사용자 ID |
| cartItemIds | Array | O | 주문할 장바구니 항목 ID 목록 (최소 1개) |
| couponId | Number | X | 사용할 쿠폰 ID (선택) |

```json
{
  "userId": 1,
  "cartItemIds": [1, 3],
  "couponId": 1
}
```

#### Response

**201 Created**: 주문이 성공적으로 생성됨

```json
{
  "code": 201,
  "message": "주문이 생성되었습니다.",
  "data": {
    "orderId": 1,
    "userId": 1,
    "totalAmount": 55000,
    "discountAmount": 5000,
    "finalAmount": 50000,
    "status": "PENDING",
    "orderItems": [
      {
        "orderItemId": 1,
        "productId": 1,
        "productName": "Macbook Pro",
        "quantity": 1,
        "unitPrice": 2000000,
        "subtotal": 2000000
      }
    ],
    "createdAt": "2025-10-29T10:00:00"
  }
}
```

#### Error Response

| 상태 코드 | 설명 | 응답 예시 |
|----------|------|-----------|
| 400 | 재고 부족 | `{"code": 400, "message": "재고가 부족합니다."}` |
| 400 | 쿠폰 무효 | `{"code": 400, "message": "쿠폰이 유효하지 않습니다."}` |
| 400 | 장바구니 비어있음 | `{"code": 400, "message": "장바구니가 비어있습니다."}` |
| 404 | 사용자 없음 | `{"code": 404, "message": "사용자를 찾을 수 없습니다."}` |

---

### 3.1.5 장바구니 전체 주문
`POST /api/orders/cart`

장바구니의 **모든** 상품을 한번에 주문합니다.

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| userId | Number | O | 사용자 ID |
| couponId | Number | X | 사용할 쿠폰 ID (선택) |

```json
{
  "userId": 1,
  "couponId": 1
}
```

#### Response

**201 Created**: 주문이 성공적으로 생성됨

```json
{
  "code": 201,
  "message": "주문이 생성되었습니다.",
  "data": {
    "orderId": 1,
    "userId": 1,
    "totalAmount": 5200000,
    "discountAmount": 520000,
    "finalAmount": 4680000,
    "status": "PENDING",
    "orderItems": [
      {
        "orderItemId": 1,
        "productId": 1,
        "productName": "Macbook Pro",
        "quantity": 2,
        "unitPrice": 2000000,
        "subtotal": 4000000
      },
      {
        "orderItemId": 2,
        "productId": 2,
        "productName": "iPhone 12",
        "quantity": 1,
        "unitPrice": 1200000,
        "subtotal": 1200000
      }
    ],
    "createdAt": "2025-10-29T10:00:00"
  }
}
```

#### Error Response

| 상태 코드 | 설명 | 응답 예시 |
|----------|------|-----------|
| 400 | 장바구니 비어있음 | `{"code": 400, "message": "장바구니가 비어있습니다."}` |
| 400 | 재고 부족 | `{"code": 400, "message": "재고가 부족합니다."}` |
| 400 | 쿠폰 무효 | `{"code": 400, "message": "쿠폰이 유효하지 않습니다."}` |
| 404 | 사용자 없음 | `{"code": 404, "message": "사용자를 찾을 수 없습니다."}` |

---

### 3.2 주문 목록 조회
`GET /api/orders`

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| userId | Number | O | - | 사용자 ID |
| page | Number | X | 0 | 페이지 번호 |
| size | Number | X | 10 | 페이지 크기 |
| status | String | X | - | 주문 상태 필터 (PENDING, COMPLETED, CANCELLED) |

#### Response

**200 OK**: 성공적으로 조회됨

```json
{
  "code": 200,
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "orders": [
      {
        "orderId": 1,
        "finalAmount": 50000,
        "status": "COMPLETED",
        "createdAt": "2025-10-29T10:00:00"
      },
      {
        "orderId": 2,
        "finalAmount": 30000,
        "status": "PENDING",
        "createdAt": "2025-10-29T11:00:00"
      }
    ],
    "totalElements": 10,
    "totalPages": 1,
    "currentPage": 0,
    "size": 10
  }
}
```

#### Error Response

| 상태 코드 | 설명 | 응답 예시 |
|----------|------|-----------|
| 400 | 잘못된 페이지 요청 | `{"code": 400, "message": "페이지 번호는 0 이상이어야 합니다."}` |
| 401 | 인증 실패 | `{"code": 401, "message": "로그인이 필요합니다."}` |
| 404 | 사용자 없음 | `{"code": 404, "message": "사용자를 찾을 수 없습니다."}` |

---

### 3.3 주문 상세 조회
`GET /api/orders/{orderId}`

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| orderId | Number | O | 주문 ID |

#### Response

**200 OK**: 성공적으로 조회됨

```json
{
  "code": 200,
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "orderId": 1,
    "userId": 1,
    "totalAmount": 55000,
    "discountAmount": 5000,
    "finalAmount": 50000,
    "status": "COMPLETED",
    "orderItems": [
      {
        "orderItemId": 1,
        "productId": 1,
        "productName": "Macbook Pro",
        "quantity": 1,
        "unitPrice": 2000000,
        "subtotal": 2000000
      }
    ],
    "createdAt": "2025-10-29T10:00:00",
    "updatedAt": "2025-10-29T10:05:00"
  }
}
```

#### Error Response

| 상태 코드 | 설명 | 응답 예시 |
|----------|------|-----------|
| 401 | 인증 실패 | `{"code": 401, "message": "로그인이 필요합니다."}` |
| 403 | 권한 없음 | `{"code": 403, "message": "다른 사용자의 주문입니다."}` |
| 404 | 주문 없음 | `{"code": 404, "message": "주문을 찾을 수 없습니다."}` |
| 404 | 사용자 없음 | `{"code": 404, "message": "사용자를 찾을 수 없습니다."}` |

---

## 4. 포인트 & 결제 (Point & Payment)

### 4.1 포인트 조회
`GET /api/points`

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| userId | Number | O | 사용자 ID |

#### Response

**200 OK**: 성공적으로 조회됨

```json
{
  "code": 200,
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "id": 1,
    "userId": 1,
    "amount": 100000,
    "updatedAt": "2025-10-29T10:00:00"
  }
}
```

#### Error Response

| 상태 코드 | 설명 | 응답 예시 |
|----------|------|-----------|
| 401 | 인증 실패 | `{"code": 401, "message": "로그인이 필요합니다."}` |
| 403 | 권한 없음 | `{"code": 403, "message": "권한이 없습니다."}` |
| 404 | 포인트 정보 없음 | `{"code": 404, "message": "포인트 정보를 찾을 수 없습니다."}` |

---

### 4.2 포인트 충전
`POST /api/points/charge`

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| userId | Number | O | 사용자 ID |
| amount | Number | O | 충전 금액 |

```json
{
  "userId": 1,
  "amount": 100000
}
```

#### Response

**200 OK**: 성공적으로 충전됨

```json
{
  "code": 200,
  "message": "포인트가 충전되었습니다.",
  "data": {
    "id": 1,
    "userId": 1,
    "amount": 200000,
    "updatedAt": "2025-10-29T10:00:00"
  }
}
```

#### Error Response

| 상태 코드 | 설명 | 응답 예시 |
|----------|------|-----------|
| 400 | 잘못된 금액 | `{"code": 400, "message": "충전 금액은 0보다 커야 합니다."}` |
| 403 | 권한 없음 | `{"code": 403, "message": "다른 사용자의 포인트입니다."}` |
| 404 | 사용자 없음 | `{"code": 404, "message": "사용자를 찾을 수 없습니다."}` |

---

### 4.3 결제 실행
`POST /api/payments`

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| userId | Number | O | 사용자 ID |
| orderId | Number | O | 주문 ID |

```json
{
  "userId": 1,
  "orderId": 1
}
```

#### Response

**200 OK**: 결제가 성공적으로 완료됨

```json
{
  "code": 200,
  "message": "결제가 완료되었습니다.",
  "data": {
    "paymentId": 1,
    "orderId": 1,
    "userId": 1,
    "finalAmount": 50000,
    "balanceAfter": 50000,
    "status": "COMPLETED",
    "paidAt": "2025-10-29T10:00:00"
  }
}
```

#### Error Response

| 상태 코드 | 설명 | 응답 예시 |
|----------|------|-----------|
| 400 | 포인트 부족 | `{"code": 400, "message": "포인트가 부족합니다."}` |
| 400 | 잘못된 주문 상태 | `{"code": 400, "message": "결제할 수 없는 주문 상태입니다."}` |
| 404 | 주문 없음 | `{"code": 404, "message": "주문을 찾을 수 없습니다."}` |
| 404 | 사용자 없음 | `{"code": 404, "message": "사용자를 찾을 수 없습니다."}` |

---

### 4.4 포인트 거래 내역 조회
`GET /api/points/transactions`

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| userId | Number | O | - | 사용자 ID |
| page | Number | X | 0 | 페이지 번호 |
| size | Number | X | 10 | 페이지 크기 |
| type | String | X | - | 거래 타입 (CHARGE, USE) |

#### Response

**200 OK**: 성공적으로 조회됨

```json
{
  "code": 200,
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "transactions": [
      {
        "id": 1,
        "type": "CHARGE",
        "amount": 100000,
        "balanceAfter": 150000,
        "createdAt": "2025-10-29T10:00:00"
      },
      {
        "id": 2,
        "type": "USE",
        "amount": 50000,
        "balanceAfter": 100000,
        "createdAt": "2025-10-29T11:00:00"
      }
    ],
    "totalElements": 20,
    "totalPages": 2,
    "currentPage": 0,
    "size": 10
  }
}
```

#### Error Response

| 상태 코드 | 설명 | 응답 예시 |
|----------|------|-----------|
| 400 | 잘못된 요청 | `{"code": 400, "message": "잘못된 요청입니다."}` |
| 401 | 인증 필요 | `{"code": 401, "message": "로그인이 필요합니다."}` |
| 404 | 사용자 없음 | `{"code": 404, "message": "사용자를 찾을 수 없습니다."}` |

---

## 5. 쿠폰 (Coupon)

### 5.1 쿠폰 발급
`POST /api/coupons/issue`

#### Request Body
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| userId | Number | O | 사용자 ID |
| couponId | Number | O | 쿠폰 ID |

```json
{
  "userId": 1,
  "couponId": 1
}
```

#### Response

**201 Created**: 쿠폰이 성공적으로 발급됨

```json
{
  "code": 201,
  "message": "쿠폰이 발급되었습니다.",
  "data": {
    "userCouponId": 1,
    "userId": 1,
    "couponId": 1,
    "couponName": "20% 할인 쿠폰",
    "discountRate": 20,
    "status": "AVAILABLE",
    "issuedAt": "2025-10-29T10:00:00",
    "expiresAt": "2025-11-29T23:59:59"
  }
}
```

#### Error Response

| 상태 코드 | 설명 | 응답 예시 |
|----------|------|-----------|
| 400 | 중복 발급 | `{"code": 400, "message": "이미 발급받은 쿠폰입니다."}` |
| 400 | 수량 소진 | `{"code": 400, "message": "쿠폰이 모두 소진되었습니다."}` |
| 400 | 유효 기간 아님 | `{"code": 400, "message": "발급 가능한 기간이 아닙니다."}` |
| 404 | 쿠폰 없음 | `{"code": 404, "message": "쿠폰을 찾을 수 없습니다."}` |
| 404 | 사용자 없음 | `{"code": 404, "message": "사용자를 찾을 수 없습니다."}` |

---

### 5.2 쿠폰 조회
`GET /api/coupons`

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| userId | Number | O | 사용자 ID |
| status | String | X | 쿠폰 상태 (AVAILABLE, USED, EXPIRED) |

#### Response

**200 OK**: 성공적으로 조회됨

```json
{
  "code": 200,
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {
    "coupons": [
      {
        "userCouponId": 1,
        "couponId": 1,
        "couponName": "20% 할인 쿠폰",
        "discountRate": 20,
        "status": "AVAILABLE",
        "issuedAt": "2025-10-29T10:00:00",
        "expiresAt": "2025-11-29T23:59:59"
      },
      {
        "userCouponId": 2,
        "couponId": 2,
        "couponName": "10% 할인 쿠폰",
        "discountRate": 10,
        "status": "USED",
        "issuedAt": "2025-10-28T10:00:00",
        "usedAt": "2025-10-29T09:00:00",
        "expiresAt": "2025-11-28T23:59:59"
      }
    ]
  }
}
```

#### Error Response

| 상태 코드 | 설명 | 응답 예시 |
|----------|------|-----------|
| 400 | 잘못된 요청 | `{"code": 400, "message": "잘못된 요청입니다."}` |
| 403 | 권한 없음 | `{"code": 403, "message": "권한이 없습니다."}` |
| 404 | 사용자 없음 | `{"code": 404, "message": "사용자를 찾을 수 없습니다."}` |

---

## 📊 공통 응답 구조

모든 API는 다음과 같은 공통 응답 구조를 사용합니다:

```json
{
  "code": 200,
  "message": "요청 처리 메시지",
  "data": {
    // 응답 데이터
  }
}
```

### HTTP 상태 코드

| 코드 | 설명 |
|------|------|
| 200 | OK - 요청 성공 |
| 201 | Created - 리소스 생성 성공 |
| 204 | No Content - 요청 성공 (응답 본문 없음) |
| 400 | Bad Request - 잘못된 요청 |
| 401 | Unauthorized - 인증 필요 |
| 403 | Forbidden - 권한 없음 |
| 404 | Not Found - 리소스 없음 |
| 500 | Internal Server Error - 서버 오류 |

