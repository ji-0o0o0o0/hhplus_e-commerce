# E-Commerce Platform - 항해플러스 과제

## 프로젝트 개요

레이어드 아키텍처 기반의 이커머스 플랫폼 백엔드 시스템입니다.
InMemory 방식으로 데이터를 관리하며, 동시성 제어를 통해 안정적인 주문/쿠폰 발급 처리를 보장합니다.

## 기술 스택

- Java 17
- Spring Boot 3.5.7
- Gradle 8.14.3
- JUnit 5 (테스트)
- JaCoCo (코드 커버리지)

## 아키텍처

### 레이어드 아키텍처 (4계층)

```
Presentation Layer (Controller)
    ↓
Application Layer (Service)
    ↓
Domain Layer (Entity, Value Object)
    ↓
Infrastructure Layer (Repository)
```

### 주요 도메인

- **Product**: 상품 관리 (재고 차감/증가)
- **Order**: 주문 생성 및 상태 관리
- **Coupon**: 선착순 쿠폰 발급
- **Point**: 포인트 충전/사용
- **Cart**: 장바구니 관리
- **Payment**: 결제 처리

---

## 동시성 제어 구현 (Step 6)

### 📌 핵심 문제

이커머스 시스템에서 다음과 같은 동시성 문제가 발생할 수 있습니다:

1. **선착순 쿠폰 발급**: 100명이 동시에 50개 한정 쿠폰을 신청하면 51개 이상 발급될 수 있음 (Race Condition)
2. **재고 차감**: 재고 50개인 상품에 100명이 동시에 주문하면 음수 재고가 발생할 수 있음
3. **포인트 사용**: 동일 사용자가 동시에 포인트를 사용하면 잔액이 음수가 될 수 있음

---

## 🔒 최종 구현: LockManager 컴포넌트 (횡단 관심사 분리)

### 왜 LockManager를 만들었나?

동시성 제어는 **횡단 관심사(Cross-cutting Concern)** 입니다.
- Logging, Transaction, Security와 같은 범주
- 여러 Service에서 공통으로 필요한 기능
- 별도 컴포넌트로 분리하는 것이 원칙

### Before (초기 구현) ❌

각 Service마다 Lock 관리 코드가 중복:

```java
// CouponService.java
public class CouponService {
    private final Map<Long, Lock> couponLocks = new ConcurrentHashMap<>();

    public UserCoupon issueCoupon(Long userId, Long couponId) {
        Lock lock = couponLocks.computeIfAbsent(couponId, id -> new ReentrantLock());
        lock.lock();
        try {
            // 쿠폰 발급 로직
        } finally {
            lock.unlock();
        }
    }
}

// ProductService.java
public class ProductService {
    private final Map<Long, Lock> productLocks = new ConcurrentHashMap<>();  // 중복!

    public void decreaseStock(Long productId, Integer quantity) {
        Lock lock = productLocks.computeIfAbsent(productId, id -> new ReentrantLock());
        lock.lock();
        try {
            // 재고 차감 로직
        } finally {
            lock.unlock();
        }
    }
}
```

**문제점:**
- ❌ Lock 관리 코드가 각 Service마다 중복
- ❌ Redis Lock으로 전환 시 모든 Service 수정 필요
- ❌ Service가 Lock 책임까지 가짐 (단일 책임 원칙 위반)

---

### After (개선) ⭐

LockManager 컴포넌트로 분리:

```java
// LockManager.java (공통 컴포넌트)
@Component
public class LockManager {
    private final Map<String, Lock> locks = new ConcurrentHashMap<>();

    public <T> T executeWithLock(String key, Supplier<T> action) {
        Lock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    public void executeWithLock(String key, Runnable action) {
        Lock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }
}
```
📁 **전체 코드**: `src/main/java/common/lock/LockManager.java`

---

### 1. 쿠폰 발급 동시성 제어

```java
// CouponService.java (간결해짐!)
@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final LockManager lockManager;  // LockManager만 주입

    public UserCoupon issueCoupon(Long userId, Long couponId) {
        return lockManager.executeWithLock("coupon:" + couponId, () -> {
            // 1. 쿠폰 조회
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

            // 2. 중복 발급 확인
            userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                    .ifPresent(uc -> {
                        throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
                    });

            // 3. 발급 가능 여부 확인
            if (!coupon.canIssue()) {
                throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
            }

            // 4. 유효기간 확인
            if (!coupon.isValid()) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
            }

            // 5. 쿠폰 발급 수량 증가
            coupon.increaseIssuedQuantity();
            couponRepository.save(coupon);

            // 6. 사용자 쿠폰 생성
            UserCoupon userCoupon = UserCoupon.issue(userId, coupon);
            return userCouponRepository.save(userCoupon);
        });
    }
}
```
📁 **전체 코드**: `src/main/java/coupon/application/CouponService.java:25`

**핵심 메커니즘**:
- `lockManager.executeWithLock("coupon:1", () -> { ... })` 형태로 간결하게 사용
- 쿠폰 ID별 독립적인 락 (쿠폰 A와 쿠폰 B는 동시 처리 가능)
- `try-finally` 자동 처리로 락 해제 보장
- 동일 쿠폰에 대한 모든 요청이 순차적으로 처리됨

**테스트 결과**:
```
✅ 100명이 50개 한정 쿠폰 신청 → 정확히 50명만 성공
✅ 1000명이 100개 한정 쿠폰 신청 → 정확히 100명만 성공
✅ 동일 사용자 10번 중복 신청 → 1번만 성공
```

---

### 2. 재고 차감 동시성 제어

```java
// ProductService.java
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final LockManager lockManager;  // LockManager만 주입

    public void decreaseStock(Long productId, Integer quantity) {
        lockManager.executeWithLock("product:" + productId, () -> {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

            product.decreaseStock(quantity);
            productRepository.save(product);
        });
    }

    public void increaseStock(Long productId, Integer quantity) {
        lockManager.executeWithLock("product:" + productId, () -> {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

            product.increaseStock(quantity);
            productRepository.save(product);
        });
    }
}
```
📁 **전체 코드**: `src/main/java/product/application/ProductService.java:44`

**핵심 메커니즘**:
- 상품 ID별로 독립적인 락 관리 (다른 상품은 동시 처리 가능)
- 재고 조회 → 검증 → 차감 과정이 원자적으로 수행
- `OrderService`에서 이 메서드를 호출하여 동시성 보장

**OrderService에서 사용**:
```java
// OrderService.java
lockManager.executeWithLock("product:" + product.getId(), () -> {
    if (product.getStock() < cartItem.getQuantity()) {
        throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
    }
    product.decreaseStock(cartItem.getQuantity());
    productRepository.save(product);
});
```
📁 **전체 코드**: `src/main/java/order/application/OrderService.java:57`

**테스트 결과**:
```
✅ 100명이 재고 50개 상품 주문 → 정확히 50개만 차감, 50명만 성공
✅ 1000명이 재고 100개 상품 주문 → 정확히 100개만 차감
✅ 동시 재고 증가 100회 → 정확히 100개 증가
✅ 차감/증가 혼합 작업 → 정확히 계산됨
```

---

## LockManager의 장점

| 항목 | Before (각 Service) | After (LockManager) |
|------|---------------------|---------------------|
| **코드 중복** | 각 Service마다 Lock 코드 | 한 곳에만 존재 ✅ |
| **책임 분리** | Service가 Lock도 관리 | Lock 책임 분리 ✅ |
| **확장성** | Redis Lock 전환 시 모든 Service 수정 | LockManager만 수정 ✅ |
| **재사용성** | 낮음 | 높음 ✅ |
| **테스트** | 각 Service 테스트마다 설정 | 공통 Mock 재사용 ✅ |
| **유지보수** | 여러 곳 수정 필요 | 한 곳만 수정 ✅ |

---

## 동시성 제어 방식 비교 분석

### 1. LockManager + ReentrantLock ⭐ 현재 선택

**장점**:
- ✅ **정확성 보장**: Race Condition 완전 차단
- ✅ **코드 재사용**: 한 번 구현으로 모든 곳에서 사용
- ✅ **확장성**: Redis Lock 전환 시 LockManager만 수정
- ✅ **DB 독립적**: InMemory 환경에서도 동작
- ✅ **세밀한 제어**: Key별 독립적인 락으로 성능 최적화
- ✅ **책임 분리**: 횡단 관심사를 별도 컴포넌트로 관리

**단점**:
- ⚠️ **대기 시간**: 동시 요청 시 순차 처리로 응답 시간 증가
- ⚠️ **데드락 위험**: 여러 락을 획득할 때 순서 관리 필요
- ⚠️ **단일 서버 제한**: 분산 환경에서는 Redis 등 외부 락 필요

**적합한 상황**:
- ✅ 선착순 이벤트 (정확한 수량 제한 필수)
- ✅ 재고 차감 (음수 재고 절대 불가)
- ✅ 단일 서버 환경 또는 InMemory 저장소
- ✅ 여러 도메인에서 동일한 동시성 제어 필요

---

### 2. synchronized 키워드

**장점**:
- ✅ JVM 레벨 지원으로 안정적
- ✅ 코드가 간결 (`synchronized` 키워드만 추가)

**단점**:
- ⚠️ **메서드 전체 락**: 세밀한 제어 불가능
- ⚠️ **확장성 부족**: 쿠폰 ID별 락 불가능 (모든 쿠폰에 하나의 락)
- ⚠️ **공정성 없음**: 대기 순서 보장 안 됨

**예시**:
```java
public synchronized UserCoupon issueCoupon(Long userId, Long couponId) {
    // 모든 쿠폰 발급 요청이 순차 처리됨 (쿠폰 ID 무관)
}
```

**선택하지 않은 이유**:
- 쿠폰 A와 쿠폰 B를 동시에 발급할 수 없어 성능 저하
- ReentrantLock이 더 유연하고 성능이 좋음

---

### 3. Optimistic Lock (낙관적 락)

**장점**:
- ✅ **높은 동시성**: 락 없이 대부분 성공
- ✅ **성능 우수**: 충돌이 적으면 빠름

**단점**:
- ⚠️ **재시도 필요**: 충돌 시 사용자가 재요청해야 함
- ⚠️ **선착순 부적합**: 충돌이 많으면 사용자 경험 나쁨
- ⚠️ **버전 관리 필요**: Entity에 `@Version` 필드 추가

**예시**:
```java
@Entity
public class Coupon {
    @Version
    private Long version;
}

// 충돌 시 OptimisticLockException 발생
```

**선택하지 않은 이유**:
- 선착순 쿠폰은 충돌이 매우 많아 재시도 폭증
- 사용자에게 "다시 시도하세요" 메시지는 나쁜 UX

---

### 4. Database Lock (SELECT FOR UPDATE)

**장점**:
- ✅ **분산 환경 지원**: 여러 서버에서 동시 접근 가능
- ✅ **트랜잭션 보장**: DB 트랜잭션과 함께 사용

**단점**:
- ⚠️ **DB 의존적**: InMemory 환경에서 사용 불가
- ⚠️ **성능 저하**: DB 락 경합 시 대기 시간 증가
- ⚠️ **데드락 위험**: 여러 테이블 락 시 데드락 발생 가능

**예시**:
```sql
SELECT * FROM coupon WHERE id = ? FOR UPDATE;
```

**선택하지 않은 이유**:
- 과제 요구사항이 InMemory 구현
- DB를 사용하지 않는 환경에서 테스트 필요

---

### 5. Redis Distributed Lock

**장점**:
- ✅ **분산 환경 완벽 지원**: 다중 서버에서 안전
- ✅ **타임아웃 설정**: 락 점유 시간 제한 가능
- ✅ **고성능**: Redis의 빠른 응답 속도
- ✅ **LockManager 패턴 적용 가능**: 인터페이스만 변경하면 전환 가능

**단점**:
- ⚠️ **외부 의존성**: Redis 서버 필요
- ⚠️ **복잡도 증가**: Redisson 등 라이브러리 필요
- ⚠️ **네트워크 지연**: Redis 통신 오버헤드

**예시 (LockManager 확장)**:
```java
@Component
public class RedisLockManager implements LockManager {
    private final RedissonClient redissonClient;

    public <T> T executeWithLock(String key, Supplier<T> action) {
        RLock lock = redissonClient.getLock(key);
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
```

**선택하지 않은 이유**:
- 과제 범위를 벗어남 (InMemory 구현 요구)
- 단일 서버 환경에서는 ReentrantLock으로 충분
- **하지만 LockManager 패턴 덕분에 쉽게 전환 가능** ⭐

---

## 동시성 제어 방식 선택 가이드

| 상황 | 추천 방식 | 이유 |
|------|----------|------|
| 선착순 이벤트 (단일 서버) | LockManager + ReentrantLock | 정확성 + 성능 + 재사용 |
| 선착순 이벤트 (분산 서버) | LockManager + Redis Lock | 분산 환경 지원 |
| 재고 차감 (높은 정확성) | LockManager + ReentrantLock | Race Condition 방지 |
| 조회수 증가 (낮은 정확성) | Optimistic Lock | 성능 우선 |
| 포인트 충전 (충돌 적음) | Optimistic Lock | 빠른 처리 |

---

## 인기 상품 집계 로직

**구현 위치**: `ProductService.getTopProducts()`

```java
public List<Product> getTopProducts() {
    LocalDateTime startDate = LocalDateTime.now().minusDays(3);
    return productRepository.findTopSellingProducts(startDate, 5);
}
```

**집계 기준**:
- 최근 3일간 판매량 기준 상위 5개 상품
- `InMemoryProductRepository`의 `salesRecord`에서 판매 기록 집계
- 주문 완료 시 `recordSale(productId, quantity)` 호출하여 누적

📁 **전체 코드**: `src/main/java/product/infrastructure/InMemoryProductRepository.java:73`

---

## 테스트 커버리지

### 전체 커버리지: **73%** ✅

```bash
./gradlew test jacocoTestReport
```

리포트 위치: `build/reports/jacoco/test/html/index.html`

### 동시성 테스트

**위치**: `src/test/java/concurrency/`

#### 1. CouponIssueConcurrencyTest
- ✅ 100명 동시 50개 한정 쿠폰 발급
- ✅ 1000명 동시 100개 한정 쿠폰 발급
- ✅ 동일 사용자 중복 발급 방지

#### 2. ProductStockConcurrencyTest
- ✅ 100명 동시 재고 50개 상품 구매
- ✅ 1000명 동시 재고 100개 상품 구매
- ✅ 동시 재고 증가 테스트
- ✅ 재고 차감/증가 혼합 테스트

---

## 실행 방법

### 애플리케이션 실행
```bash
./gradlew bootRun
```

### 전체 테스트 실행
```bash
./gradlew test
```

### 동시성 테스트만 실행
```bash
./gradlew test --tests "com.hhplus.hhplus_ecommerce.concurrency.*"
```

### 커버리지 리포트 생성
```bash
./gradlew test jacocoTestReport
```

---

## API 문서

Swagger UI: `http://localhost:8080/swagger-ui.html`

### 주요 API

#### 상품
- `GET /api/products` - 상품 목록 조회
- `GET /api/products/{id}` - 상품 상세 조회
- `GET /api/products/popular` - 인기 상품 TOP 5

#### 쿠폰
- `POST /api/coupons/{couponId}/issue` - 쿠폰 발급 (선착순)
- `GET /api/coupons/available` - 발급 가능한 쿠폰 목록

#### 주문
- `POST /api/orders` - 주문 생성
- `GET /api/orders/{orderId}` - 주문 조회

#### 결제
- `POST /api/payments` - 결제 실행

---

## 프로젝트 구조

```
src/main/java/com/hhplus/hhplus_ecommerce/
├── common/
│   ├── lock/
│   │   └── LockManager.java           # 동시성 제어 공통 컴포넌트 ⭐
│   └── exception/
│       ├── BusinessException.java
│       └── ErrorCode.java
├── product/
│   ├── domain/Product.java
│   ├── application/ProductService.java
│   ├── infrastructure/InMemoryProductRepository.java
│   └── controller/ProductController.java
├── coupon/
│   ├── domain/Coupon.java
│   ├── application/CouponService.java
│   ├── infrastructure/InMemoryCouponRepository.java
│   └── controller/CouponController.java
├── order/
│   ├── domain/Order.java
│   ├── application/OrderService.java
│   ├── infrastructure/InMemoryOrderRepository.java
│   └── controller/OrderController.java
└── ...

src/test/java/com/hhplus/hhplus_ecommerce/
├── concurrency/                        # 동시성 테스트 ⭐
│   ├── CouponIssueConcurrencyTest.java
│   └── ProductStockConcurrencyTest.java
└── ...
```


