# Step 16. Transaction Diagnosis & Domain Separation

`트랜잭션 분리 시 발생 가능한 문제 분석 및 결과적 일관성 설계 문서`

---

## 1. 개요

### 1.1 배경 및 목적

현재 이커머스 시스템은 모놀리식 아키텍처로 구성되어 있으며, 모든 도메인이 단일 데이터베이스를 공유하고 있습니다. 비즈니스 확장에 따라 특정 도메인(주문, 결제, 상품)에 트래픽이 집중될 경우, 전체 시스템의 성능 저하와 장애 전파 위험이 있습니다.

본 문서는 서비스 확장에 대비하여 **도메인별 서버 및 데이터베이스 분리** 시 발생 가능한 트랜잭션 문제를 분석하고, **분산 트랜잭션 환경에서 데이터 일관성을 보장**하기 위한 설계 방안을 제시합니다.

### 1.2 현재 시스템 구조 (Monolithic)

```
┌─────────────────────────────────────────────┐
│         E-commerce Application              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │  Order   │  │ Payment  │  │ Product  │  │
│  │ Service  │  │ Service  │  │ Service  │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  │
│       │             │             │         │
│       └─────────────┼─────────────┘         │
│                     │                       │
│              ┌──────▼──────┐                │
│              │  @Transactional              │
│              │  (ACID 보장)                 │
│              └──────┬──────┘                │
└─────────────────────┼───────────────────────┘
                      │
              ┌───────▼────────┐
              │  MySQL Database │
              │                 │
              │ - orders        │
              │ - payments      │
              │ - products      │
              │ - users         │
              │ - coupons       │
              └─────────────────┘
```

**현재 주문-결제 플로우 (단일 트랜잭션)**
```java
@Transactional
public PaymentResponse executePayment(Long orderId) {
    // 1. 주문 조회
    Order order = orderRepository.findById(orderId);

    // 2. 포인트 차감 (users 테이블)
    pointService.usePoint(userId, amount);

    // 3. 쿠폰 사용 처리 (user_coupons 테이블)
    couponService.useCoupon(userId, couponId);

    // 4. 주문 상태 변경 (orders 테이블)
    order.complete();

    // 5. 결제 정보 저장 (payments 테이블)
    Payment payment = paymentRepository.save(...);

    return payment;
    // ✅ 모든 작업이 같은 DB 트랜잭션 내에서 ACID 보장
}
```

---

## 2. 도메인 분리 전략

### 2.1 도메인 경계 식별 (Bounded Context)

이커머스 시스템을 **DDD(Domain-Driven Design)** 관점에서 분석하여 다음과 같이 도메인을 식별했습니다:

| 도메인 | 핵심 엔티티 | 주요 책임 | 분리 우선순위 |
|--------|------------|----------|--------------|
| **주문(Order)** | Order, OrderItem | 주문 생성/취소, 주문 조회 | 🔥 High |
| **결제(Payment)** | Payment, Point | 결제 처리, 포인트 관리 | 🔥 High |
| **상품(Product)** | Product, ProductRanking | 상품 조회, 재고 관리, 인기 상품 집계 | 🔥 High |
| **쿠폰(Coupon)** | Coupon, UserCoupon | 쿠폰 발급/사용 | 🟡 Medium |
| **사용자(User)** | User | 회원 정보 관리 | 🟢 Low |
| **장바구니(Cart)** | CartItem | 장바구니 관리 | 🟢 Low |

### 2.2 배포 단위 분리 설계

**Phase 1: 고부하 도메인 우선 분리**

트래픽 집중도와 독립성을 고려하여 다음 순서로 분리를 진행합니다:

```
1단계: Product Service 분리
  └─ 이유: 상품 조회 트래픽이 가장 높음 (읽기 중심)
  └─ 의존성: 낮음 (다른 도메인에 영향 적음)

2단계: Order Service 분리
  └─ 이유: 주문 생성/조회 로직이 복잡하고 트랜잭션이 김
  └─ 의존성: 높음 (Product, Payment, Coupon 참조)

3단계: Payment Service 분리
  └─ 이유: 결제 처리는 보안/격리 필요
  └─ 의존성: 중간 (Order 참조, Point 관리)
```

**Phase 2: MSA 아키텍처 전환 후**

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Order Service  │     │ Payment Service │     │ Product Service │
│                 │     │                 │     │                 │
│ - 주문 생성     │     │ - 결제 처리     │     │ - 상품 조회     │
│ - 주문 조회     │     │ - 포인트 관리   │     │ - 재고 관리     │
│ - 주문 취소     │     │ - 결제 내역     │     │ - 인기도 집계   │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
    ┌────▼─────┐            ┌────▼─────┐           ┌────▼─────┐
    │ Order DB │            │ Payment  │           │ Product  │
    │          │            │    DB    │           │    DB    │
    │ orders   │            │ payments │           │ products │
    │ order_   │            │ points   │           │ product_ │
    │  items   │            │          │           │ rankings │
    └──────────┘            └──────────┘           └──────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                        ┌────────▼────────┐
                        │  Message Broker │
                        │   (Kafka/Redis) │
                        │                 │
                        │ - 이벤트 발행   │
                        │ - 비동기 통신   │
                        └─────────────────┘
```

### 2.3 데이터베이스 스키마 전략

**현재 설계 (모놀리식, 이미 MSA 대비)**

본 프로젝트는 **초기 설계 단계부터 MSA 전환을 고려**하여 다음과 같은 원칙을 적용했습니다:

```sql
-- V1__Create_Initial_Schema.sql
-- 설계 원칙:
-- 1. FK 제약조건 사용 안 함 (성능, 데드락 방지, 샤딩 대비)
-- 2. 애플리케이션 레벨에서 참조 무결성 관리
-- 3. 인덱스는 유지 (조회 성능을 위해)
```

**실제 스키마 (현재)**

```sql
-- 주문 테이블
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT,  -- ✅ FK 없음 (논리적 참조만)
    total_amount BIGINT NOT NULL,
    discount_amount BIGINT NOT NULL DEFAULT 0,
    final_amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,

    INDEX idx_orders_user_status (user_id, status),
    INDEX idx_orders_coupon (coupon_id)  -- INDEX만 있고 FK는 없음
);

-- 주문 상품 테이블 (이미 반정규화 적용)
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,  -- ✅ 반정규화 (스냅샷)
    unit_price BIGINT NOT NULL,          -- ✅ 주문 시점 가격 저장
    quantity INT NOT NULL,
    subtotal BIGINT NOT NULL,

    INDEX idx_order_items_order (order_id),
    INDEX idx_order_items_product (product_id)
    -- ✅ FK 없음! 애플리케이션에서 관리
);

-- 상품 테이블
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    price BIGINT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,  -- 낙관적 락

    INDEX idx_products_category (category)
);
```

**MSA 전환 시 변경 사항**

현재 설계는 이미 FK를 사용하지 않으므로, MSA 전환 시 **테이블 구조 변경 없이 DB만 물리적으로 분리**하면 됩니다:

```
현재 (모놀리식):
┌─────────────────────────────┐
│  MySQL Database (단일)      │
│  - orders                   │
│  - order_items (반정규화)   │
│  - products                 │
│  - payments                 │
└─────────────────────────────┘

MSA 전환 후:
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  Order DB    │  │  Product DB  │  │  Payment DB  │
│  - orders    │  │  - products  │  │  - payments  │
│  - order_    │  │              │  │  - points    │
│    items     │  │              │  │              │
└──────────────┘  └──────────────┘  └──────────────┘
```

**정규화 vs 반정규화 트레이드오프**

| 구분 | 정규화 (FK 유지) | 반정규화 (데이터 복제) |
|------|-----------------|---------------------|
| **참조 무결성** | ✅ DB 레벨 보장 | ❌ 애플리케이션에서 보장 필요 |
| **데이터 일관성** | ✅ 즉시 반영 | ⚠️ 결과적 일관성 |
| **조회 성능** | ❌ JOIN 필요 | ✅ 단일 테이블 조회 |
| **MSA 적합성** | ❌ 불가능 | ✅ 가능 |

**선택: 반정규화 (데이터 복제)**
- 주문 시점의 상품명/가격은 변경되지 않아야 함 (히스토리 보존)
- 상품 서비스 장애 시에도 주문 조회 가능
- 조인 없이 빠른 조회 가능

---

## 3. 분산 트랜잭션 문제 분석

### 3.1 기존 모놀리식 트랜잭션 흐름 (ACID 보장)

```java
@Transactional  // 단일 DB 트랜잭션
public PaymentResponse executePayment(Long orderId) {
    Order order = orderRepository.findById(orderId);  // orders 테이블

    pointService.usePoint(userId, amount);            // points 테이블
    couponService.useCoupon(userId, couponId);        // user_coupons 테이블
    order.complete();                                 // orders 테이블 UPDATE

    Payment payment = paymentRepository.save(...);    // payments 테이블

    return payment;
    // ✅ Commit: 모든 작업 성공
    // ✅ Rollback: 하나라도 실패 시 전체 롤백
}
```

**ACID 특성**
- **Atomicity**: 모든 작업이 성공하거나 모두 실패
- **Consistency**: DB 제약조건 위반 시 자동 롤백
- **Isolation**: 다른 트랜잭션과 격리
- **Durability**: Commit 시 영구 저장

### 3.2 MSA 환경에서의 트랜잭션 분리 문제

**문제 1: 분산 트랜잭션 불가 (2PC 한계)**

```
Order Service                Payment Service             Product Service
     │                            │                           │
     │ [1] 주문 생성              │                           │
     │─────────────────▶          │                           │
     │                            │ [2] 포인트 차감           │
     │                            │───────────────▶           │
     │                            │                           │ [3] 재고 차감
     │                            │                           │─────────────▶
     │                            │                           │
     │                            │                  ❌ [4] 재고 부족 실패!
     │                            │◀──────────────────────────┘
     │                            │
     │                    ⚠️ [5] 포인트는 이미 차감됨!
     │                            │
     │            ⚠️ [6] 주문도 이미 생성됨!
     │                            │
```

**문제점**:
- 각 서비스는 독립적인 DB 트랜잭션 사용
- 하나의 서비스가 실패해도 다른 서비스는 이미 커밋됨
- **부분 실패(Partial Failure)** 발생 → 데이터 불일치

**문제 2: 2단계 커밋(2PC)의 한계**

```
┌──────────────────────────────────────────────┐
│  2PC (Two-Phase Commit) 방식                 │
├──────────────────────────────────────────────┤
│  Phase 1: Prepare (투표)                     │
│    Coordinator → 모든 참여자에게 "준비됐나?" 물음│
│    참여자들 → "OK" 응답                       │
│                                              │
│  Phase 2: Commit (실행)                      │
│    Coordinator → "Commit 해라" 명령          │
│    참여자들 → 실제 커밋 수행                 │
└──────────────────────────────────────────────┘

❌ 문제점:
1. 블로킹 프로토콜 (Coordinator 응답 대기 시 락 보유)
2. Single Point of Failure (Coordinator 다운 시 전체 중단)
3. 성능 저하 (네트워크 왕복 2번, 락 보유 시간 김)
4. MSA 철학과 맞지 않음 (서비스 간 강결합)
```

**문제 3: 네트워크 파티션과 타임아웃**

```
Order Service           Payment Service
     │                       │
     │  결제 요청 전송        │
     │──────────────────▶    │
     │                       │ ✅ 결제 성공 (포인트 차감)
     │                       │
     │  ⏰ 응답 타임아웃      │ 📡 응답 전송 중 네트워크 끊김
     │  (응답 못 받음)       │
     │                       │
     │  ❌ "결제 실패"로 판단 │
     │                       │
     │                       ⚠️ 실제로는 결제 성공했는데
     │                          Order Service는 실패로 인식!
```

**문제 4: 중복 처리 위험**

```
사용자 요청 → Order Service
                   │
                   │ [1] 주문 생성 API 호출
                   │──────────────────────▶ Payment Service
                   │                             │
                   │                             │ 포인트 차감 중...
                   │                             │
                   │ ⏰ 타임아웃                  │
                   │                             │
                   │ [2] 재시도!                 │
                   │──────────────────────▶      │
                                                 │
                                        ⚠️ 포인트 2번 차감 위험!
```

### 3.3 구체적인 실패 시나리오

**시나리오 1: 재고 차감 실패 (중간 단계 실패)**

```
Step 1: 주문 생성 (Order Service)     ✅ 성공 → Commit
Step 2: 재고 차감 (Product Service)   ❌ 실패 (재고 부족)
Step 3: 포인트 차감 (Payment Service) ⏹️ 실행 안 됨

결과: 주문은 생성됐지만 재고는 차감 안 됨 → 불일치!
```

**시나리오 2: 결제 실패 (마지막 단계 실패)**

```
Step 1: 주문 생성 (Order Service)     ✅ 성공 → Commit
Step 2: 재고 차감 (Product Service)   ✅ 성공 → Commit
Step 3: 포인트 차감 (Payment Service) ❌ 실패 (잔액 부족)

결과: 주문은 생성되고 재고는 차감됐지만 결제는 실패 → 심각한 불일치!
```

**시나리오 3: 네트워크 타임아웃**

```
Order Service → Payment Service 호출
                      │
                      │ ✅ 포인트 차감 성공
                      │ 📤 응답 전송
                      │
                ⏰ 네트워크 지연으로 타임아웃
                      │
❌ Order Service는 "실패"로 판단
                      │
                ⚠️ 실제로는 성공했지만 주문 상태는 "실패"
```

**시나리오 4: 서비스 장애**

```
주문 생성 중 Payment Service 다운
  │
  ├─ 옵션 1: 주문 생성 중단 → ❌ 사용자 경험 나쁨
  ├─ 옵션 2: 주문은 생성, 결제는 나중에 → ⚠️ 미결제 주문 증가
  └─ 옵션 3: 동기 → 비동기 전환 → ✅ 결과적 일관성
```

---

## 4. 분산 트랜잭션 설계: Saga 패턴

### 4.1 Saga 패턴 개요

Saga 패턴은 분산 트랜잭션을 **여러 개의 로컬 트랜잭션**으로 분해하고, 각 단계마다 **보상 트랜잭션(Compensation)**을 정의하여 결과적 일관성을 보장하는 패턴입니다.

```
Saga = Local Transaction 1 → Local Transaction 2 → ... → Local Transaction N

실패 시: Compensation N-1 ← Compensation N-2 ← ... ← Compensation 1
```

### 4.2 Choreography vs Orchestration 비교

**Orchestration 방식 (중앙 관리자)**

```
                    ┌───────────────────┐
                    │ Order Saga Manager│ ← 중앙 제어
                    │  (Orchestrator)   │
                    └─────────┬─────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
        ┌──────────┐    ┌──────────┐   ┌──────────┐
        │  Order   │    │ Product  │   │ Payment  │
        │ Service  │    │ Service  │   │ Service  │
        └──────────┘    └──────────┘   └──────────┘

흐름:
1. Orchestrator → Order Service: 주문 생성 명령
2. Orchestrator → Product Service: 재고 차감 명령
3. Orchestrator → Payment Service: 결제 처리 명령
4. 실패 시 Orchestrator가 보상 트랜잭션 실행
```

**장점**
- ✅ 이벤트 흐름 가시성 확보 (중앙에서 전체 플로우 파악)
- ✅ 복잡한 비즈니스 로직 처리 용이
- ✅ 모니터링 단순 (Orchestrator만 보면 됨)
- ✅ 순환 의존성 위험 낮음

**단점**
- ❌ **SPOF (Single Point of Failure)**: Orchestrator 다운 시 전체 중단
- ❌ Orchestrator 부하 집중 (트래픽 증가 시 증설 필요)
- ❌ 별도 인프라 구성 비용
- ❌ 서비스 간 결합도 증가

---

**Choreography 방식 (이벤트 기반)**

```
┌──────────┐         ┌──────────┐         ┌──────────┐
│  Order   │         │ Product  │         │ Payment  │
│ Service  │         │ Service  │         │ Service  │
└────┬─────┘         └────┬─────┘         └────┬─────┘
     │                    │                    │
     │ OrderCreated       │                    │
     │ Event 발행         │                    │
     │────────────────────▶                    │
     │                    │                    │
     │                    │ 재고 차감          │
     │                    │ StockDecreased     │
     │                    │ Event 발행         │
     │                    │────────────────────▶
     │                    │                    │
     │                    │                    │ 결제 처리
     │                    │                    │
     │◀────────────────────────────────────────┘
     │              PaymentCompleted Event
     │
```

**장점**
- ✅ **SPOF 없음** (중앙 관리자 없음)
- ✅ 서비스 독립성 높음 (느슨한 결합)
- ✅ 확장성 우수 (서비스별 독립 스케일링)
- ✅ 인프라 비용 절감

**단점**
- ❌ 이벤트 흐름 파악 어려움 (서비스 증가 시)
- ❌ 순환 의존성 발생 가능
- ❌ 분산 추적 도구 필요 (Sleuth, Zipkin)
- ❌ 디버깅 복잡

### 4.3 선택: Choreography 방식 + 그 근거

**선택 이유**

1. **SPOF 회피가 최우선**
   - Orchestrator 장애 시 전체 주문 시스템 마비
   - 이커머스 특성상 주문 중단은 치명적

2. **초기 서비스 규모**
   - 현재 6개 도메인으로 이벤트 흐름 복잡도 낮음
   - Orchestrator 도입 대비 개발 비용 낮음

3. **확장성**
   - 특정 도메인(상품 조회 등) 트래픽 급증 시 독립 스케일링 가능
   - Orchestrator 없어 병목 지점 없음

4. **MSA 철학 부합**
   - 서비스 간 느슨한 결합 유지
   - 각 서비스가 자율적으로 이벤트 구독

**단점 보완 방안**

| 단점 | 보완 방안 |
|-----|----------|
| 이벤트 흐름 파악 어려움 | - Kafka 토픽 네이밍 규칙 정립<br>- Spring Cloud Sleuth + Zipkin 분산 추적<br>- 이벤트 스토밍 결과 문서화 |
| 순환 의존성 위험 | - 이벤트 방향 명확히 정의 (단방향)<br>- 도메인 간 의존성 매트릭스 관리 |
| 디버깅 복잡 | - 구조화된 로깅 (JSON 포맷)<br>- correlation ID 전파 |

### 4.4 이벤트 기반 주문-결제 흐름 설계

**정상 흐름 (Happy Path)**

```
사용자 → Order Service
            │
            │ [1] 주문 생성 (PENDING 상태)
            │     orders.status = 'PENDING'
            │     ✅ Local Transaction Commit
            │
            │ [2] OrderCreatedEvent 발행
            │     { orderId, userId, items, couponId }
            │
            ├──────────────────┬──────────────────┐
            │                  │                  │
            ▼                  ▼                  ▼
      Product Service    Payment Service   Coupon Service
            │                  │                  │
      [3] 재고 차감          [4] 포인트 차감     [5] 쿠폰 사용
      products.stock -= qty  points.balance -= amount
            │                  │                  │
      ✅ Commit           ✅ Commit           ✅ Commit
            │                  │                  │
      [6] StockDecreasedEvent  [7] PaymentCompletedEvent
            │                  │
            └──────────────────┴──────────────────▶
                                │
                      Order Service (이벤트 수신)
                                │
                      [8] 주문 완료 처리
                      orders.status = 'COMPLETED'
                                │
                            ✅ 최종 완료
```

**실패 및 보상 흐름 (MSA 전환 후 - 보상 트랜잭션)**

> ⚠️ **주의**: 아래 흐름은 **MSA 전환 후 설계**입니다. 현재 모놀리식 코드에서는 PaymentService가 실패 시 `BusinessException`을 던져 `@Transactional`이 자동 롤백합니다. PaymentFailedEvent를 발행하지 않습니다. (Section 5.2 "현재 코드 vs MSA 전환 후 비교" 참조)

```
사용자 → Order Service
            │
            │ [1] 주문 생성 (PENDING)
            │ ✅ Commit
            │
            │ [2] OrderCreatedEvent 발행
            │
            ├──────────────────┬──────────────────┐
            │                  │                  │
            ▼                  ▼                  ▼
      Product Service    Payment Service   Coupon Service
            │                  │                  │
      [3] 재고 차감          [4] 포인트 차감     [5] 쿠폰 사용
      ✅ Commit           ❌ FAIL (잔액 부족)  ✅ Commit
            │                  │                  │
            │            [6] PaymentFailedEvent 발행
            │                  │
            ◀──────────────────┴──────────────────┘
            │
      [7] 보상 트랜잭션 시작
            │
            ├─────────────────┬─────────────────┐
            │                 │                 │
      [8] 재고 복구       Order Service     Coupon Service
      products.stock += qty    │            [9] 쿠폰 복구
      ✅ Commit         [10] 주문 취소     user_coupons.used=false
                        orders.status =     ✅ Commit
                        'PAYMENT_FAILED'
                        ✅ Commit
```

### 4.5 이벤트 정의

**OrderCreatedEvent**
```json
{
  "eventId": "uuid-123",
  "eventType": "OrderCreated",
  "timestamp": "2025-01-15T10:30:00Z",
  "aggregateId": "order-456",
  "payload": {
    "orderId": 456,
    "userId": 789,
    "items": [
      { "productId": 1, "quantity": 2, "price": 50000 }
    ],
    "couponId": 10,
    "totalAmount": 100000,
    "discountAmount": 10000,
    "finalAmount": 90000
  }
}
```

**PaymentCompletedEvent** (기존 STEP 15 구현)
```java
public record PaymentCompletedEvent(
    Long orderId,
    Long userId,
    Long totalAmount,
    Long discountAmount,
    Long finalAmount,
    Long couponId,
    List<OrderItemInfo> items,
    LocalDateTime completedAt
) {
    public record OrderItemInfo(
        Long productId,
        String productName,
        Integer quantity,
        Long unitPrice
    ) {}
}
```

**PaymentFailedEvent** (보상 트랜잭션용)
```json
{
  "eventId": "uuid-789",
  "eventType": "PaymentFailed",
  "timestamp": "2025-01-15T10:30:05Z",
  "aggregateId": "order-456",
  "payload": {
    "orderId": 456,
    "userId": 789,
    "reason": "INSUFFICIENT_BALANCE",
    "failedAmount": 90000,
    "items": [
      { "productId": 1, "quantity": 2 }
    ],
    "couponId": 10
  }
}
```

**StockRestoredEvent** (재고 복구 확인용)
```json
{
  "eventId": "uuid-101",
  "eventType": "StockRestored",
  "timestamp": "2025-01-15T10:30:06Z",
  "aggregateId": "product-1",
  "payload": {
    "productId": 1,
    "restoredQuantity": 2,
    "currentStock": 150,
    "relatedOrderId": 456
  }
}
```

---

## 5. 보상 트랜잭션 설계

### 5.1 보상 트랜잭션 원칙

1. **멱등성 보장**: 같은 이벤트 여러 번 처리해도 결과 동일
2. **순서 보장**: 정방향 트랜잭션의 역순으로 보상
3. **부분 보상 가능**: 실패한 지점부터만 보상
4. **보상 불가능한 작업은 마지막에**: 취소 불가능한 작업(외부 API 호출 등)은 후순위

### 5.2 시나리오별 보상 트랜잭션

**시나리오 1: 재고 차감 실패 (첫 단계 실패)**

```
정방향:
[1] 주문 생성 ✅ → [2] 재고 차감 ❌ (재고 부족)

보상:
[1] 주문 취소 (상태 변경: PENDING → STOCK_UNAVAILABLE)

코드:
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleStockDecreaseFailed(StockDecreaseFailedEvent event) {
    Order order = orderRepository.findById(event.orderId());
    order.cancel(OrderCancelReason.STOCK_UNAVAILABLE);
    orderRepository.save(order);

    log.warn("[보상] 재고 부족으로 주문 취소 - OrderId: {}", event.orderId());
}
```

**시나리오 2: 결제 실패 (중간 단계 실패)**

```
정방향:
[1] 주문 생성 ✅ → [2] 재고 차감 ✅ → [3] 결제 처리 ❌ (잔액 부족)

보상:
[3] 재고 복구 ← [2] 쿠폰 복구 ← [1] 주문 취소
```

**현재 코드 (모놀리식) vs MSA 전환 후 비교**

```java
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 현재 코드 (모놀리식 - 단일 트랜잭션)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Transactional  // ← 모든 작업이 하나의 트랜잭션
public PaymentResponse executePaymentWithResponse(Long userId, Long orderId) {
    Order order = orderRepository.findById(orderId)...;

    validatePayment(order, userId);

    // 포인트 차감
    pointService.usePointWithDistributedLock(userId, order.getFinalAmount());
    // ❌ 실패 시: BusinessException 발생 → 전체 롤백
    // ✅ 이벤트 발행 불필요! @Transactional이 자동 롤백

    // 쿠폰 사용
    if (order.getCouponId() != null) {
        UserCoupon userCoupon = ...;
        userCoupon.use();
        userCouponRepository.save(userCoupon);
    }

    // 주문 완료
    order.complete();
    orderRepository.save(order);

    // ✅ 성공 시에만 이벤트 발행
    publishPaymentCompletedEvent(order);

    return new PaymentResponse(...);
    // 예외 발생 시 모든 작업 자동 롤백 (재고, 쿠폰, 주문 모두)
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MSA 전환 후 (분산 트랜잭션 - 보상 트랜잭션 필요)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Payment Service
@Transactional
public void processPayment(OrderCreatedEvent event) {
    try {
        // 포인트 차감 (Payment Service의 로컬 트랜잭션)
        pointService.usePoint(event.userId(), event.finalAmount());

        // ✅ 성공 시: PaymentCompletedEvent 발행
        PaymentCompletedEvent successEvent = new PaymentCompletedEvent(...);
        eventPublisher.publishEvent(successEvent);

    } catch (InsufficientBalanceException e) {
        // ❌ 실패 시: PaymentFailedEvent 발행 (보상 트랜잭션 트리거)
        PaymentFailedEvent failedEvent = new PaymentFailedEvent(
            event.orderId(),
            event.userId(),
            "INSUFFICIENT_BALANCE",
            event.items(),
            event.couponId()
        );
        eventPublisher.publishEvent(failedEvent);

        log.warn("[결제 실패] 포인트 부족 - OrderId: {}, UserId: {}",
            event.orderId(), event.userId());
    }
    // ⚠️ 예외를 던지지 않음! 이벤트로만 실패 통지
}

// Product Service: 재고 복구
@TransactionalEventListener
public void handlePaymentFailed(PaymentFailedEvent event) {
    for (var item : event.items()) {
        Product product = productRepository.findById(item.productId());
        product.increaseStock(item.quantity());
        productRepository.save(product);
    }

    eventPublisher.publishEvent(new StockRestoredEvent(event.orderId()));
}

// Coupon Service: 쿠폰 복구
@TransactionalEventListener
public void handlePaymentFailed(PaymentFailedEvent event) {
    if (event.couponId() != null) {
        UserCoupon coupon = userCouponRepository.findByUserIdAndCouponId(
            event.userId(), event.couponId());
        coupon.restore();  // used = false
        userCouponRepository.save(coupon);
    }
}

// Order Service: 주문 취소
@TransactionalEventListener
public void handlePaymentFailed(PaymentFailedEvent event) {
    Order order = orderRepository.findById(event.orderId());
    order.cancel(OrderCancelReason.PAYMENT_FAILED);
    orderRepository.save(order);
}
```

**시나리오 3: 외부 API 타임아웃 (불확실한 상태)**

```
상황: Payment Service 호출했지만 응답 타임아웃
  - 실제로는 성공했을 수도, 실패했을 수도 있음

해결책:
1. 멱등키(Idempotency Key) 사용
   - 주문 ID를 멱등키로 활용
   - Payment Service에서 중복 요청 체크

2. 상태 조회 API 제공
   - GET /payments/{orderId}/status
   - 타임아웃 발생 시 상태 확인 후 판단

3. 재시도 전략
   - 타임아웃 시 최대 3회 재시도
   - 지수 백오프 (1초, 2초, 4초)

코드:
// 멱등성 보장
@Transactional
public Payment processPayment(OrderCreatedEvent event) {
    // 중복 처리 체크
    Payment existing = paymentRepository.findByOrderId(event.orderId());
    if (existing != null) {
        log.warn("[멱등성] 이미 처리된 주문 - OrderId: {}", event.orderId());
        return existing;  // 기존 결과 반환
    }

    // 결제 처리
    pointService.usePoint(event.userId(), event.finalAmount());
    Payment payment = Payment.create(event.orderId(), event.finalAmount());
    return paymentRepository.save(payment);
}
```

### 5.3 보상 트랜잭션 재시도 전략

```java
@Component
public class CompensationRetryHandler {

    private static final int MAX_RETRY = 3;
    private static final long INITIAL_DELAY = 1000L;  // 1초

    @Retryable(
        retryFor = {DataAccessException.class, OptimisticLockException.class},
        maxAttempts = MAX_RETRY,
        backoff = @Backoff(delay = INITIAL_DELAY, multiplier = 2)
    )
    public void restoreStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.increaseStock(quantity);
        productRepository.save(product);
    }

    @Recover
    public void recoverStockRestore(DataAccessException e, Long productId, int quantity) {
        // 재시도 실패 시 DLQ로 전송
        CompensationFailedEvent event = new CompensationFailedEvent(
            "STOCK_RESTORE_FAILED",
            productId,
            quantity,
            e.getMessage()
        );
        dlqPublisher.publish(event);

        // 알람 발송
        alertService.sendAlert("재고 복구 실패", productId, quantity);
    }
}
```

---

## 6. 데이터 일관성 보장 방안

### 6.1 결과적 일관성 (Eventual Consistency)

**강한 일관성 vs 결과적 일관성**

```
┌─────────────────────────────────────────────────────────┐
│  강한 일관성 (Strong Consistency)                       │
│  - 모놀리식 단일 트랜잭션                                │
│  - 언제 조회해도 최신 데이터                             │
│  - 예: 주문 완료 직후 조회 → 즉시 "완료" 상태            │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  결과적 일관성 (Eventual Consistency)                   │
│  - 분산 트랜잭션 (이벤트 기반)                           │
│  - 일시적으로 불일치 상태 존재                           │
│  - 시간이 지나면 일관된 상태로 수렴                      │
│  - 예: 주문 완료 직후 조회 → "처리중" → "완료"           │
└─────────────────────────────────────────────────────────┘
```

**타임라인 예시**

```
T0: 주문 생성 (Order Service)
    └─ orders.status = 'PENDING'

T1: OrderCreatedEvent 발행

T2: Product Service 이벤트 수신
    └─ products.stock -= 2

T3: Payment Service 이벤트 수신
    └─ points.balance -= 90000

T4: PaymentCompletedEvent 발행

T5: Order Service 이벤트 수신
    └─ orders.status = 'COMPLETED'

┌────────────────────────────────────────────┐
│  T0 ~ T5 사이: 불일치 구간                 │
│  - 주문은 PENDING인데 재고는 이미 차감     │
│  - 사용자가 T2 시점에 조회하면 혼란 가능   │
└────────────────────────────────────────────┘
```

### 6.2 멱등성(Idempotency) 보장

**문제: 이벤트 중복 처리**

```
Kafka에서 at-least-once 보장
  └─ 같은 이벤트가 여러 번 전달될 수 있음

예:
OrderCreatedEvent (orderId=123) 처리 중 Consumer 재시작
  └─ 같은 이벤트 다시 수신
      └─ 재고가 2번 차감될 위험!
```

**해결: 멱등성 키 활용**

```java
// 이벤트 처리 이력 테이블
CREATE TABLE processed_events (
    event_id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_event_type_processed_at (event_type, processed_at)
);

// 멱등성 체크
@Transactional
public void handleOrderCreated(OrderCreatedEvent event) {
    // 중복 처리 체크
    boolean alreadyProcessed = processedEventRepository
        .existsByEventId(event.getEventId());

    if (alreadyProcessed) {
        log.warn("[멱등성] 이미 처리된 이벤트 - EventId: {}", event.getEventId());
        return;  // 중복 처리 방지
    }

    // 실제 비즈니스 로직
    Product product = productRepository.findById(event.getProductId());
    product.decreaseStock(event.getQuantity());
    productRepository.save(product);

    // 처리 이력 저장 (같은 트랜잭션)
    ProcessedEvent processedEvent = new ProcessedEvent(
        event.getEventId(),
        event.getEventType(),
        LocalDateTime.now()
    );
    processedEventRepository.save(processedEvent);

    // ✅ Commit: 재고 차감 + 이력 저장이 함께 커밋됨
}
```

**대안: 비즈니스 키 활용**

```java
// 주문 ID를 멱등키로 활용
@Transactional
public void processPayment(OrderCreatedEvent event) {
    // 이미 처리된 주문인지 확인
    Payment existing = paymentRepository.findByOrderId(event.getOrderId());

    if (existing != null) {
        log.warn("[멱등성] 이미 결제된 주문 - OrderId: {}", event.getOrderId());
        return;
    }

    // 결제 처리
    pointService.usePoint(event.getUserId(), event.getFinalAmount());

    Payment payment = Payment.create(event.getOrderId(), event.getFinalAmount());
    paymentRepository.save(payment);
}
```

### 6.3 이벤트 순서 보장

**문제: 이벤트 순서 뒤바뀜**

```
정상 순서:
[1] OrderCreatedEvent → [2] PaymentCompletedEvent

문제 상황:
[2] PaymentCompletedEvent 먼저 도착
    └─ Order가 아직 생성 안 됐는데 완료 처리 시도 → 에러!

원인:
- Kafka 파티션 다를 경우
- 네트워크 지연
- Consumer 재시작
```

**해결책 1: 파티션 키 설정**

```java
// Kafka Producer에서 orderId를 파티션 키로 사용
public void publishEvent(OrderCreatedEvent event) {
    kafkaTemplate.send(
        "order-events",               // 토픽
        event.getOrderId().toString(), // 파티션 키 (같은 orderId는 같은 파티션)
        event                          // 이벤트
    );
}

// 효과: 같은 주문의 이벤트는 순서 보장됨
```

**해결책 2: 버전 관리**

```java
// 이벤트에 버전 정보 포함
public record OrderEvent(
    String eventId,
    Long orderId,
    Long version,  // 순서 보장용
    String eventType,
    Object payload
) {}

// Consumer에서 버전 체크
@Transactional
public void handleEvent(OrderEvent event) {
    Order order = orderRepository.findById(event.getOrderId());

    if (order.getVersion() + 1 != event.getVersion()) {
        // 순서가 맞지 않으면 재시도 큐로 이동
        retryQueue.add(event);
        return;
    }

    // 정상 처리
    order.applyEvent(event);
    order.incrementVersion();
    orderRepository.save(order);
}
```

**해결책 3: 대기 큐 (Waiting Queue)**

```java
// 선행 이벤트가 없으면 대기
@Component
public class EventSequenceHandler {

    private final Map<Long, Queue<OrderEvent>> waitingEvents = new ConcurrentHashMap<>();

    @TransactionalEventListener
    public void handleOrderEvent(OrderEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
            .orElse(null);

        if (order == null && event.getEventType().equals("PaymentCompleted")) {
            // 주문이 아직 없으면 대기
            waitingEvents
                .computeIfAbsent(event.getOrderId(), k -> new LinkedList<>())
                .add(event);

            log.warn("[순서 보장] 주문 미생성으로 대기 - OrderId: {}", event.getOrderId());
            return;
        }

        // 정상 처리
        processEvent(event);

        // 대기 중인 후속 이벤트 처리
        Queue<OrderEvent> waiting = waitingEvents.get(event.getOrderId());
        if (waiting != null) {
            while (!waiting.isEmpty()) {
                OrderEvent waitingEvent = waiting.poll();
                processEvent(waitingEvent);
            }
        }
    }
}
```

### 6.4 타임아웃 및 재시도 전략

**재시도 정책**

```java
@Configuration
public class EventRetryConfig {

    @Bean
    public RetryTemplate eventRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // 재시도 정책: 최대 3회, 지수 백오프
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);    // 1초
        backOffPolicy.setMultiplier(2.0);          // 2배씩 증가
        backOffPolicy.setMaxInterval(10000);       // 최대 10초

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);

        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}

// 사용
@Service
public class ProductEventHandler {

    @Autowired
    private RetryTemplate eventRetryTemplate;

    @TransactionalEventListener
    public void handleStockDecrease(OrderCreatedEvent event) {
        eventRetryTemplate.execute(context -> {
            Product product = productRepository.findById(event.getProductId());
            product.decreaseStock(event.getQuantity());
            return productRepository.save(product);
        });
    }
}
```

**재시도 실패 시 DLQ 전송**

```java
@Service
public class EventProcessorWithDLQ {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener
    public void handleEvent(OrderCreatedEvent event) {
        try {
            retryTemplate.execute(context -> {
                processEvent(event);
                return null;
            });
        } catch (Exception e) {
            // 재시도 실패 → DLQ 전송
            sendToDLQ(event, e);
        }
    }

    private void sendToDLQ(OrderCreatedEvent event, Exception error) {
        DLQMessage dlqMessage = new DLQMessage(
            event,
            error.getMessage(),
            LocalDateTime.now(),
            3  // 재시도 횟수
        );

        kafkaTemplate.send("order-events-dlq", dlqMessage);

        log.error("[DLQ] 이벤트 처리 실패 - EventId: {}, Error: {}",
            event.getEventId(), error.getMessage());
    }
}
```

---

## 7. 모니터링 및 장애 대응

### 7.1 DLQ (Dead Letter Queue) 패턴

**DLQ 구조**

```
┌────────────────────────────────────────────────┐
│  이벤트 처리 흐름                              │
└────────────────────────────────────────────────┘

이벤트 발행 → Consumer 처리
                 │
                 ├─ ✅ 성공 → 완료
                 │
                 ├─ ⚠️ 재시도 가능 오류
                 │    └─ Retry Queue (최대 3회)
                 │            │
                 │            ├─ ✅ 성공 → 완료
                 │            │
                 │            └─ ❌ 재시도 실패
                 │                   │
                 └─ ❌ 재시도 불가 오류
                                     │
                            ┌────────▼────────┐
                            │  Dead Letter    │
                            │     Queue       │
                            └────────┬────────┘
                                     │
                      ┌──────────────┼──────────────┐
                      │              │              │
                      ▼              ▼              ▼
              재처리 Consumer   알림 Consumer    분석 도구
              (수동 재처리)    (개발자 통지)  (장애 패턴 분석)
```

**DLQ 메시지 구조**

```java
@Getter
@AllArgsConstructor
public class DLQMessage {
    private String originalEventId;
    private String eventType;
    private Object originalPayload;
    private String errorMessage;
    private String stackTrace;
    private int retryCount;
    private LocalDateTime firstAttemptAt;
    private LocalDateTime lastAttemptAt;
    private String failureReason;  // TIMEOUT, DB_ERROR, VALIDATION_ERROR 등
}
```

**DLQ Consumer (모니터링)**

```java
@Component
public class DLQMonitor {

    @KafkaListener(topics = "order-events-dlq")
    public void monitorDLQ(DLQMessage message) {
        log.error("[DLQ 모니터] 처리 실패 이벤트 감지\n" +
            "EventId: {}\n" +
            "EventType: {}\n" +
            "FailureReason: {}\n" +
            "RetryCount: {}\n" +
            "Error: {}",
            message.getOriginalEventId(),
            message.getEventType(),
            message.getFailureReason(),
            message.getRetryCount(),
            message.getErrorMessage()
        );

        // Slack 알림
        slackClient.sendAlert(
            "#order-alerts",
            String.format("🚨 DLQ 알림: %s 이벤트 처리 실패 (재시도 %d회)",
                message.getEventType(), message.getRetryCount())
        );

        // 메트릭 기록
        meterRegistry.counter("dlq.events",
            "event_type", message.getEventType(),
            "failure_reason", message.getFailureReason()
        ).increment();

        // DB 저장 (분석용)
        dlqRepository.save(message);
    }
}
```

**DLQ 재처리 (수동)**

```java
@RestController
@RequestMapping("/admin/dlq")
public class DLQAdminController {

    // DLQ 이벤트 목록 조회
    @GetMapping
    public List<DLQMessage> getDLQMessages(
        @RequestParam(required = false) String eventType,
        @RequestParam(required = false) String failureReason
    ) {
        return dlqRepository.findByFilters(eventType, failureReason);
    }

    // 수동 재처리
    @PostMapping("/{eventId}/retry")
    public ResponseEntity<?> retryEvent(@PathVariable String eventId) {
        DLQMessage dlqMessage = dlqRepository.findById(eventId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DLQ_MESSAGE_NOT_FOUND));

        // 원본 이벤트 재발행
        Object originalEvent = dlqMessage.getOriginalPayload();
        eventPublisher.publishEvent(originalEvent);

        // DLQ에서 제거
        dlqRepository.deleteById(eventId);

        return ResponseEntity.ok("재처리 시작");
    }

    // 일괄 재처리
    @PostMapping("/retry-all")
    public ResponseEntity<?> retryAllDLQ(
        @RequestParam String eventType,
        @RequestParam String failureReason
    ) {
        List<DLQMessage> messages = dlqRepository
            .findByEventTypeAndFailureReason(eventType, failureReason);

        for (DLQMessage message : messages) {
            eventPublisher.publishEvent(message.getOriginalPayload());
            dlqRepository.deleteById(message.getOriginalEventId());
        }

        return ResponseEntity.ok(messages.size() + "건 재처리 시작");
    }
}
```

### 7.2 분산 추적 (Distributed Tracing)

**문제: 분산 환경에서 요청 추적 어려움**

```
사용자 요청 → Order Service
                   │
                   │ (어디까지 처리됐지?)
                   ├─────────────────┬─────────────────┐
                   │                 │                 │
             Product Service   Payment Service   Coupon Service
                   │                 │                 │
                   ?                 ?                 ?
```

**해결: Spring Cloud Sleuth + Zipkin**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  sleuth:
    sampler:
      probability: 1.0  # 100% 추적 (운영에서는 0.1 권장)
  zipkin:
    base-url: http://localhost:9411
```

**추적 정보 전파**

```java
@Component
public class TraceableEventPublisher {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private Tracer tracer;  // Sleuth Tracer

    public void publishEvent(Object event) {
        // 현재 Trace ID 가져오기
        String traceId = tracer.currentSpan().context().traceIdString();
        String spanId = tracer.currentSpan().context().spanIdString();

        // 이벤트에 Trace 정보 포함
        if (event instanceof TraceableEvent traceableEvent) {
            traceableEvent.setTraceId(traceId);
            traceableEvent.setSpanId(spanId);
        }

        eventPublisher.publishEvent(event);

        log.info("[이벤트 발행] TraceId: {}, EventType: {}", traceId, event.getClass().getSimpleName());
    }
}

// Consumer에서 Trace 정보 복원
@Component
public class TraceableEventListener {

    @Autowired
    private Tracer tracer;

    @TransactionalEventListener
    public void handleEvent(TraceableEvent event) {
        // Trace Context 복원
        TraceContext context = TraceContext.newBuilder()
            .traceId(event.getTraceId())
            .spanId(event.getSpanId())
            .build();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(tracer.nextSpan(context))) {
            processEvent(event);
        }
    }
}
```

**Zipkin UI에서 확인**

```
http://localhost:9411

타임라인 예시:
┌──────────────────────────────────────────────────┐
│ Trace ID: 1a2b3c4d5e6f                           │
├──────────────────────────────────────────────────┤
│ order-service    POST /orders      [100ms]       │
│   ├─ product-service  decrease-stock  [30ms]     │
│   ├─ payment-service  process-payment [50ms]     │
│   └─ coupon-service   use-coupon      [20ms]     │
├──────────────────────────────────────────────────┤
│ Total Duration: 200ms                            │
└──────────────────────────────────────────────────┘

실패 지점 확인:
  payment-service: ❌ Error: INSUFFICIENT_BALANCE
    └─ 보상 트랜잭션 시작
        ├─ product-service: stock-restore ✅
        └─ coupon-service: coupon-restore ✅
```

### 7.3 로깅 및 알림 전략

**구조화된 로깅 (JSON 포맷)**

```java
@Slf4j
@Component
public class StructuredLogger {

    public void logEvent(String eventType, String action, Object data, String status) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", LocalDateTime.now());
        logData.put("eventType", eventType);
        logData.put("action", action);
        logData.put("data", data);
        logData.put("status", status);
        logData.put("traceId", MDC.get("traceId"));

        log.info("{}", new ObjectMapper().writeValueAsString(logData));
    }
}

// 사용
structuredLogger.logEvent(
    "OrderCreated",
    "STOCK_DECREASE",
    Map.of("productId", 1, "quantity", 2),
    "SUCCESS"
);

// 출력 (JSON):
{
  "timestamp": "2025-01-15T10:30:00",
  "eventType": "OrderCreated",
  "action": "STOCK_DECREASE",
  "data": { "productId": 1, "quantity": 2 },
  "status": "SUCCESS",
  "traceId": "1a2b3c4d5e6f"
}
```

**알림 임계값 설정**

```java
@Component
public class EventMonitor {

    private final AtomicInteger failureCount = new AtomicInteger(0);

    @Scheduled(fixedRate = 60000)  // 1분마다
    public void checkFailureRate() {
        int failures = failureCount.getAndSet(0);

        if (failures > 10) {
            slackClient.sendAlert(
                "#order-critical",
                String.format("🚨 경고: 최근 1분간 이벤트 처리 실패 %d건", failures)
            );
        }
    }

    public void recordFailure() {
        failureCount.incrementAndGet();
    }
}
```

**Prometheus 메트릭**

```java
@Component
public class EventMetrics {

    private final Counter eventPublishedCounter;
    private final Counter eventProcessedCounter;
    private final Counter eventFailedCounter;
    private final Timer eventProcessingTime;

    public EventMetrics(MeterRegistry meterRegistry) {
        this.eventPublishedCounter = Counter.builder("events.published")
            .tag("type", "order")
            .register(meterRegistry);

        this.eventProcessedCounter = Counter.builder("events.processed")
            .tag("type", "order")
            .register(meterRegistry);

        this.eventFailedCounter = Counter.builder("events.failed")
            .tag("type", "order")
            .register(meterRegistry);

        this.eventProcessingTime = Timer.builder("events.processing.time")
            .tag("type", "order")
            .register(meterRegistry);
    }

    public void recordEventPublished() {
        eventPublishedCounter.increment();
    }

    public void recordEventProcessed(long durationMs) {
        eventProcessedCounter.increment();
        eventProcessingTime.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordEventFailed() {
        eventFailedCounter.increment();
    }
}
```

---

## 8. 사용자 경험 고려사항

### 8.1 비동기 처리와 사용자 피드백

**문제: 주문 완료 시점의 모호함**

```
동기 방식 (기존):
사용자 요청 → 주문 생성 → 재고 차감 → 결제 → 응답 "주문 완료!"
                                              └─ 이 시점에 모든 것이 확정됨

비동기 방식 (MSA):
사용자 요청 → 주문 생성 → 응답 "주문 접수!"
                              └─ 재고 차감은 진행 중...
                              └─ 결제도 진행 중...

⚠️ 사용자: "주문 접수가 뭐야? 완료된 거야 말 거야?"
```

**해결: 명확한 상태 표시**

```java
public enum OrderStatus {
    PENDING("주문 접수", "결제 처리 중입니다"),
    PAYMENT_PROCESSING("결제 진행중", "잠시만 기다려주세요"),
    COMPLETED("주문 완료", "주문이 확정되었습니다"),
    PAYMENT_FAILED("결제 실패", "포인트 잔액이 부족합니다"),
    STOCK_UNAVAILABLE("재고 부족", "상품 재고가 부족합니다"),
    CANCELLED("주문 취소", "주문이 취소되었습니다");

    private final String displayName;
    private final String description;
}

// API 응답
{
  "orderId": 123,
  "status": "PENDING",
  "statusDisplay": "주문 접수",
  "message": "결제 처리 중입니다. 잠시만 기다려주세요.",
  "estimatedCompletionTime": "2025-01-15T10:30:05Z",  // 예상 완료 시간
  "canCancel": true  // 취소 가능 여부
}
```

**프론트엔드 UI 가이드**

```javascript
// 주문 생성 후 폴링으로 상태 확인
async function createOrder(orderData) {
  const response = await fetch('/api/orders', {
    method: 'POST',
    body: JSON.stringify(orderData)
  });

  const order = await response.json();

  // 즉시 "주문 접수" 표시
  showOrderStatus(order.orderId, 'PENDING');

  // 상태 폴링 시작 (최대 30초)
  const maxAttempts = 30;
  let attempts = 0;

  const interval = setInterval(async () => {
    const status = await fetch(`/api/orders/${order.orderId}/status`);
    const statusData = await status.json();

    if (statusData.status === 'COMPLETED') {
      clearInterval(interval);
      showOrderSuccess(statusData);
    } else if (statusData.status.includes('FAILED')) {
      clearInterval(interval);
      showOrderFailure(statusData);
    } else if (attempts++ > maxAttempts) {
      clearInterval(interval);
      showProcessingTooLong(statusData);
    }
  }, 1000);  // 1초마다 확인
}
```

### 8.2 재고 상태 동기화 문제

**문제: 주문 생성 후 재고 차감 지연**

```
T0: 사용자 A가 상품 조회 (재고 1개)
T1: 사용자 A가 주문 생성 (PENDING)
T2: 사용자 B가 상품 조회 (아직 재고 1개로 보임!)
    └─ 재고 차감 이벤트가 아직 처리 안 됨
T3: 사용자 B도 주문 생성 시도
    └─ 실제로는 재고 없는데 주문 생성됨
T4: 재고 차감 이벤트 처리 (재고 0)
T5: 보상 트랜잭션으로 사용자 B 주문 취소

⚠️ 사용자 B: "주문됐다가 취소됐어요!"
```

**해결책 1: 예약된 재고(Reserved Stock) 개념 도입**

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100),
    stock INT NOT NULL,                  -- 실제 재고
    reserved_stock INT DEFAULT 0,        -- 예약된 재고
    available_stock INT GENERATED ALWAYS AS (stock - reserved_stock) -- 가용 재고
);
```

```java
// 주문 생성 시 즉시 재고 예약
@Transactional
public Order createOrder(Long userId, List<CartItem> items) {
    // 1. 재고 예약 (실제 차감 아님)
    for (CartItem item : items) {
        Product product = productRepository.findById(item.getProductId());

        if (product.getAvailableStock() < item.getQuantity()) {
            throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
        }

        product.reserveStock(item.getQuantity());  // reserved_stock += qty
        productRepository.save(product);
    }

    // 2. 주문 생성
    Order order = Order.create(userId, items);
    orderRepository.save(order);

    // 3. OrderCreatedEvent 발행
    eventPublisher.publishEvent(new OrderCreatedEvent(order));

    return order;
}

// 결제 완료 시 예약 → 실제 차감 전환
@TransactionalEventListener
public void handlePaymentCompleted(PaymentCompletedEvent event) {
    for (var item : event.getItems()) {
        Product product = productRepository.findById(item.getProductId());
        product.confirmReservation(item.getQuantity());  // stock -= qty, reserved_stock -= qty
        productRepository.save(product);
    }
}

// 결제 실패 시 예약 해제
@TransactionalEventListener
public void handlePaymentFailed(PaymentFailedEvent event) {
    for (var item : event.getItems()) {
        Product product = productRepository.findById(item.getProductId());
        product.releaseReservation(item.getQuantity());  // reserved_stock -= qty
        productRepository.save(product);
    }
}
```

**효과**:
- 사용자가 조회하는 재고는 `available_stock` (실제 재고 - 예약 재고)
- 주문 생성 시 즉시 예약되어 다른 사용자가 중복 주문 불가
- 결제 완료/실패에 따라 예약 → 확정 또는 예약 해제

**해결책 2: 낙관적 UI (Optimistic UI)**

```javascript
// 주문 생성 시 즉시 재고 차감된 것처럼 UI 업데이트
function createOrder(productId, quantity) {
  // 1. UI 즉시 업데이트 (낙관적)
  updateProductStock(productId, -quantity);
  showMessage('주문 처리 중...');

  // 2. API 호출
  fetch('/api/orders', { method: 'POST', body: ... })
    .then(response => {
      if (response.ok) {
        showMessage('주문 완료!');
      } else {
        // 실패 시 원복
        updateProductStock(productId, +quantity);
        showMessage('주문 실패: 재고 부족');
      }
    });
}
```

### 8.3 쿠폰 사용 처리

**문제: 쿠폰 사용 후 결제 실패**

```
T1: 주문 생성 (쿠폰 ID 포함)
T2: 쿠폰 사용 처리 (used = true)
    └─ 사용자가 "내 쿠폰" 화면에서 즉시 사라짐
T3: 결제 실패 (잔액 부족)
T4: 보상 트랜잭션으로 쿠폰 복구 (used = false)
    └─ 쿠폰이 다시 나타남

⚠️ 사용자: "쿠폰이 사라졌다가 다시 나타났어요!"
```

**해결: 쿠폰 상태 세분화**

```java
public enum CouponStatus {
    AVAILABLE("사용 가능"),
    RESERVED("사용 예약", "주문 진행 중"),  // 예약 상태 추가
    USED("사용 완료"),
    EXPIRED("만료됨");

    private final String displayName;
    private final String description;
}

// 주문 생성 시 쿠폰 예약
@Transactional
public Order createOrder(Long userId, Long couponId) {
    UserCoupon coupon = userCouponRepository.findById(couponId);

    if (!coupon.isAvailable()) {
        throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
    }

    coupon.reserve();  // status = RESERVED
    userCouponRepository.save(coupon);

    // 주문 생성...
}

// 결제 완료 시 확정
@TransactionalEventListener
public void handlePaymentCompleted(PaymentCompletedEvent event) {
    if (event.getCouponId() != null) {
        UserCoupon coupon = userCouponRepository.findById(event.getCouponId());
        coupon.use();  // status = USED
        userCouponRepository.save(coupon);
    }
}

// 결제 실패 시 예약 해제
@TransactionalEventListener
public void handlePaymentFailed(PaymentFailedEvent event) {
    if (event.getCouponId() != null) {
        UserCoupon coupon = userCouponRepository.findById(event.getCouponId());
        coupon.releaseReservation();  // status = AVAILABLE
        userCouponRepository.save(coupon);
    }
}
```

**UI 표시**

```html
<!-- 사용 가능 쿠폰 -->
<div class="coupon available">
  <span>10% 할인 쿠폰</span>
  <button>사용하기</button>
</div>

<!-- 예약된 쿠폰 (주문 진행 중) -->
<div class="coupon reserved">
  <span>10% 할인 쿠폰</span>
  <span class="status">주문 진행 중...</span>
  <small>결제 완료 시 사용됩니다</small>
</div>

<!-- 사용 완료 -->
<div class="coupon used">
  <span>10% 할인 쿠폰</span>
  <span class="status">사용 완료</span>
</div>
```

### 8.4 상태 조회 API 제공

**실시간 주문 상태 조회**

```java
@GetMapping("/orders/{orderId}/status")
public ResponseEntity<OrderStatusResponse> getOrderStatus(@PathVariable Long orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

    OrderStatusResponse response = OrderStatusResponse.builder()
        .orderId(order.getId())
        .status(order.getStatus())
        .statusDisplay(order.getStatus().getDisplayName())
        .message(getStatusMessage(order))
        .progress(getProgress(order))  // 진행률 (%)
        .timeline(getTimeline(order))  // 타임라인
        .canCancel(order.canCancel())
        .build();

    return ResponseEntity.ok(response);
}

private int getProgress(Order order) {
    return switch (order.getStatus()) {
        case PENDING -> 25;                 // 주문 접수
        case PAYMENT_PROCESSING -> 50;      // 결제 진행
        case STOCK_CONFIRMED -> 75;         // 재고 확인
        case COMPLETED -> 100;              // 완료
        default -> 0;
    };
}

private List<TimelineItem> getTimeline(Order order) {
    return List.of(
        new TimelineItem("주문 접수", order.getCreatedAt(), true),
        new TimelineItem("재고 확인", getStockConfirmedAt(order), order.isStockConfirmed()),
        new TimelineItem("결제 완료", getPaymentCompletedAt(order), order.isPaymentCompleted()),
        new TimelineItem("주문 확정", order.getCompletedAt(), order.isCompleted())
    );
}
```

**응답 예시**

```json
{
  "orderId": 123,
  "status": "PAYMENT_PROCESSING",
  "statusDisplay": "결제 진행중",
  "message": "결제 처리 중입니다. 잠시만 기다려주세요.",
  "progress": 50,
  "timeline": [
    { "step": "주문 접수", "timestamp": "2025-01-15T10:30:00Z", "completed": true },
    { "step": "재고 확인", "timestamp": "2025-01-15T10:30:02Z", "completed": true },
    { "step": "결제 완료", "timestamp": null, "completed": false },
    { "step": "주문 확정", "timestamp": null, "completed": false }
  ],
  "canCancel": true
}
```

---

## 9. 트레이드오프 및 의사결정 기록

### 9.1 모놀리식 vs MSA

| 고려사항 | 모놀리식 유지 | MSA 전환 |
|---------|-------------|---------|
| **데이터 일관성** | ✅ ACID 보장 | ⚠️ 결과적 일관성 |
| **개발 복잡도** | ✅ 낮음 | ❌ 높음 (이벤트, 보상 로직) |
| **배포 독립성** | ❌ 전체 재배포 | ✅ 서비스별 독립 배포 |
| **장애 격리** | ❌ 전체 영향 | ✅ 특정 서비스만 영향 |
| **확장성** | ❌ 전체 스케일링 | ✅ 부하 서비스만 스케일링 |
| **트랜잭션 관리** | ✅ 단순 | ❌ 복잡 (Saga, 보상) |
| **성능** | ✅ In-memory 호출 | ⚠️ 네트워크 오버헤드 |
| **조직 구조** | 소규모 팀 적합 | 대규모 팀, 도메인별 소유 |

**결정: 점진적 분리 전략**
- 초기: 모놀리식으로 시작하되 **관심사 분리** 철저히
- 트래픽 증가 시: 고부하 도메인(Product)부터 분리
- 이벤트 기반 구조는 미리 도입 (STEP 15)

### 9.2 Saga 패턴: Orchestration vs Choreography

| 고려사항 | Orchestration | Choreography |
|---------|--------------|--------------|
| **SPOF** | ❌ Orchestrator 장애 시 전체 중단 | ✅ 없음 |
| **가시성** | ✅ 중앙에서 플로우 파악 | ❌ 분산되어 파악 어려움 |
| **복잡도** | ✅ 중앙 집중 로직 | ⚠️ 서비스별 분산 로직 |
| **확장성** | ⚠️ Orchestrator 병목 가능 | ✅ 독립 확장 |
| **순환 의존** | ✅ 발생 안 함 | ⚠️ 발생 가능 |
| **인프라 비용** | ❌ 별도 인프라 필요 | ✅ 낮음 |

**결정: Choreography 선택**
- SPOF 회피가 최우선 (이커머스 특성상 주문 중단 치명적)
- 초기 서비스 수 적어 복잡도 관리 가능
- Sleuth + Zipkin으로 가시성 보완

### 9.3 동기 vs 비동기 통신

| 고려사항 | 동기 (REST API) | 비동기 (이벤트) |
|---------|----------------|----------------|
| **응답 시간** | ⚠️ 모든 서비스 응답 대기 | ✅ 즉시 응답 |
| **결합도** | ❌ 높음 (직접 호출) | ✅ 낮음 (이벤트 발행) |
| **에러 처리** | ✅ 즉시 확인 가능 | ⚠️ 별도 모니터링 필요 |
| **일관성** | ✅ 강한 일관성 | ⚠️ 결과적 일관성 |
| **사용자 경험** | ✅ 명확 (성공/실패) | ⚠️ 상태 조회 필요 |
| **장애 전파** | ❌ 연쇄 장애 위험 | ✅ 격리됨 |

**결정: 하이브리드 접근**
- **핵심 로직**: 동기 (주문 생성 → 재고 확인)
- **부가 로직**: 비동기 (데이터 플랫폼 전송, 인기도 집계)
- **보상 로직**: 비동기 이벤트

### 9.4 정규화 vs 반정규화

| 고려사항 | 정규화 (JOIN) | 반정규화 (데이터 복제) |
|---------|--------------|---------------------|
| **데이터 일관성** | ✅ 단일 진실 공급원 | ⚠️ 동기화 필요 |
| **조회 성능** | ⚠️ JOIN 비용 | ✅ 단일 테이블 |
| **저장 공간** | ✅ 효율적 | ❌ 중복 저장 |
| **MSA 적합성** | ❌ 서비스 간 JOIN 불가 | ✅ 독립 조회 가능 |
| **히스토리 보존** | ⚠️ 추가 로직 필요 | ✅ 시점 데이터 보존 |

**결정: 반정규화 (주문-상품)**
- `order_items` 테이블에 `product_name`, `unit_price` 복제
- 이유: 주문 시점 가격 보존 필요, 상품 서비스 장애 시에도 주문 조회 가능

---

## 10. 향후 개선 방향

### 10.1 단기 개선 (3개월 이내)

1. **Circuit Breaker 도입**
   - Resilience4j 활용
   - 외부 서비스 장애 시 빠른 실패 처리
   - Fallback 로직 구현

2. **이벤트 스키마 관리**
   - Avro/Protobuf로 이벤트 스키마 정의
   - 하위 호환성 보장

3. **배치 복구 작업**
   - DLQ 이벤트 자동 재처리 배치
   - 미완료 주문 정리 배치

### 10.2 중기 개선 (6개월 이내)

1. **Event Sourcing 도입 검토**
   - 주문 도메인의 모든 상태 변경을 이벤트로 저장
   - 언제든 과거 상태 복원 가능

2. **CQRS 패턴 적용**
   - Command (쓰기): 주문 생성, 결제 처리
   - Query (읽기): 주문 조회, 통계 (별도 Read Model)
   - 읽기 성능 최적화

3. **API Gateway 도입**
   - 서비스별 API를 단일 엔드포인트로 통합
   - 인증/인가 중앙 관리
   - Rate Limiting

### 10.3 장기 개선 (1년 이내)

1. **서비스 메시 (Service Mesh)**
   - Istio/Linkerd 도입
   - 서비스 간 통신 관리 (Retry, Timeout, Circuit Breaker)
   - 분산 추적 자동화

2. **Kafka Streams 활용**
   - 실시간 이벤트 처리
   - 복잡한 이벤트 패턴 감지
   - Stateful 집계 (주문 통계, 인기 상품 실시간 갱신)

3. **Multi-Region 배포**
   - 글로벌 서비스 확장
   - Region별 DB 복제 (Eventual Consistency)
   - CRDT (Conflict-free Replicated Data Type) 검토

---

## 11. 결론

### 11.1 핵심 요약

본 문서는 이커머스 시스템을 **모놀리식에서 MSA로 전환**할 때 발생하는 **트랜잭션 문제를 분석**하고, **Saga 패턴 기반의 분산 트랜잭션 설계**를 제시했습니다.

**주요 내용**:
1. **도메인 분리**: DDD 기반으로 Order, Payment, Product 등 6개 도메인 식별
2. **Saga 패턴**: Choreography 방식 선택 (SPOF 회피 우선)
3. **보상 트랜잭션**: 실패 시나리오별 롤백 전략 수립
4. **결과적 일관성**: 멱등성, 재시도, DLQ로 데이터 일관성 보장
5. **모니터링**: Sleuth + Zipkin 분산 추적, DLQ 모니터링
6. **사용자 경험**: 상태 세분화, 예약 재고, 진행 상황 표시

### 11.2 설계 검증 체크리스트

- [x] 도메인별 트랜잭션 분리 시 발생 가능한 문제 파악
- [x] 분산 트랜잭션 설계 (Saga 패턴)
- [x] 보상 트랜잭션 설계
- [x] 데이터 일관성 보장 방안 (멱등성, 재시도)
- [x] 모니터링 및 장애 대응 (DLQ, 분산 추적)
- [x] 사용자 경험 고려 (상태 조회, 예약 재고)
- [x] 트레이드오프 분석 및 의사결정 근거

### 11.3 프로젝트 적용 가이드

**즉시 적용 가능 (STEP 15 활용)**:
- 부가 로직(데이터 플랫폼, 인기도 집계)은 이미 이벤트로 분리됨
- `PaymentCompletedEvent`를 활용하여 보상 트랜잭션 구현 가능

**점진적 전환 로드맵**:
1. Phase 1: 이벤트 기반 아키텍처 구축 (완료)
2. Phase 2: Product Service 분리 (읽기 중심, 독립성 높음)
3. Phase 3: Order/Payment Service 분리 + Saga 패턴 적용
4. Phase 4: 모니터링 강화 (Zipkin, DLQ 자동화)

**성공 지표**:
- 주문 처리 실패율 < 0.1%
- 보상 트랜잭션 성공률 > 99.9%
- DLQ 이벤트 처리 시간 < 1시간
- 서비스별 독립 배포 가능

---

## 참고 자료

- [Saga Pattern - Chris Richardson](https://microservices.io/patterns/data/saga.html)
- [Spring Cloud Sleuth Documentation](https://spring.io/projects/spring-cloud-sleuth)
- [Kafka Idempotent Producer](https://kafka.apache.org/documentation/#idempotence)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)
- [DDD - Eric Evans, "Domain-Driven Design"](https://www.domainlanguage.com/ddd/)
