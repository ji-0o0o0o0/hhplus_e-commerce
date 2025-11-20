# STEP 09 - 동시성 문제 분석 및 해결 방안 보고서

**작성일**: 2025-01-20
**프로젝트**: HH Plus 이커머스 서비스
**작성자**: ji-0o0o0o0

---

## 목차
1. [동시성 문제 식별](#1-동시성-문제-식별)
2. [동시성 문제 분석](#2-동시성-문제-분석)
3. [해결 방안 선정](#3-해결-방안-선정)
4. [구현 계획](#4-구현-계획)

---

## 1. 동시성 문제 식별

### 1.1 이커머스 시스템의 동시성 위험 지점

이커머스 시스템에서는 다음과 같은 동시성 문제가 발생할 수 있습니다:

| 도메인 | 시나리오 | 발생 가능한 문제 | 비즈니스 영향 |
|--------|---------|---------------|--------------|
| **상품 재고** | 100명이 동시에 재고 50개 상품 구매 | 재고 초과 차감, 음수 재고 발생 | 재고 부족 상품 판매 → 고객 불만 |
| **선착순 쿠폰** | 1000명이 동시에 100개 한정 쿠폰 신청 | 쿠폰 초과 발급 | 마케팅 비용 증가 |
| **포인트 충전** | 동일 사용자가 동시에 여러 번 충전 | 포인트 누락, 이중 충전 | 고객 불만, 재무 손실 |
| **포인트 사용** | 동일 사용자가 동시에 여러 결제 | 잔액 초과 사용 | 재무 손실 |
| **주문 생성** | 동시 주문 시 재고/포인트 중복 차감 | 데이터 불일치 | 고객 불만, 재무 손실 |

### 1.2 Race Condition 발생 지점

```
Thread 1: READ 재고=50 → CHECK(50>=10) → WRITE 재고=40
Thread 2: READ 재고=50 → CHECK(50>=10) → WRITE 재고=40
                                              ↓
                                         재고 20이 되어야 하지만
                                         실제로는 40! (손실 발생)
```

---

## 2. 동시성 문제 분석

### 2.1 상품 재고 차감

#### 문제 상황
```java
// ❌ 동시성 제어 없는 코드 (문제 발생)
public void decreaseStock(Long productId, Integer quantity) {
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

    // ⚠️ 여기서 다른 스레드가 동시에 접근 가능!
    product.decreaseStock(quantity);
    productRepository.save(product);
}
```

**시나리오**: 재고 10개 상품에 100명이 동시 주문
- **예상 결과**: 10명 성공, 90명 실패
- **실제 결과**: 50명 성공 (재고 -40개), 데이터 정합성 깨짐

#### 데이터베이스 격리 수준의 한계

| 격리 수준 | Lost Update 방지 | Dirty Read 방지 | 비고 |
|----------|----------------|----------------|-----|
| READ_UNCOMMITTED | ❌ | ❌ | 사용 불가 |
| READ_COMMITTED | ❌ | ✅ | MySQL 기본값이지만 부족 |
| REPEATABLE_READ | ⚠️ | ✅ | MySQL InnoDB 기본, 하지만 Write Skew 발생 가능 |
| SERIALIZABLE | ✅ | ✅ | 성능 저하 심각 |

**결론**: 격리 수준만으로는 불충분, **DB Lock이 필수**

---

### 2.2 선착순 쿠폰 발급

#### 문제 상황
```java
// ❌ 동시성 제어 없는 코드
public UserCoupon issueCoupon(Long userId, Long couponId) {
    Coupon coupon = couponRepository.findById(couponId)
        .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

    // ⚠️ 100명이 동시에 canIssue()를 통과할 수 있음
    if (!coupon.canIssue()) {
        throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
    }

    coupon.increaseIssuedQuantity();
    couponRepository.save(coupon);

    return userCouponRepository.save(UserCoupon.issue(userId, coupon));
}
```

**시나리오**: 100개 한정 쿠폰에 1000명이 동시 신청
- **예상 결과**: 100명 성공, 900명 실패
- **실제 결과**: 150명 성공 (50개 초과 발급)

#### 문제 원인
1. **Check-Then-Act 패턴**: 확인(canIssue)과 실행(increaseIssuedQuantity) 사이에 간격 존재
2. **원자성 부재**: 여러 스레드가 동시에 canIssue()를 통과
3. **DB 격리 수준 무용**: 단순 SELECT는 락을 걸지 않음

---

### 2.3 포인트 충전/사용

#### 문제 상황 1: 포인트 충전
```java
// ❌ 동시성 제어 없는 코드
public Point chargePoint(Long userId, Long amount) {
    Point point = pointRepository.findByUserId(userId)
        .orElseGet(() -> Point.create(userId));

    // ⚠️ 동시 충전 시 덮어쓰기 발생
    point.charge(amount);
    return pointRepository.save(point);
}
```

**시나리오**: 잔액 1000원에 동시에 2번 충전(각 1000원)
- **예상 결과**: 3000원
- **실제 결과**: 2000원 (1번 충전 손실)

#### 문제 상황 2: 포인트 사용
```java
// ❌ 동시성 제어 없는 코드
public void usePoint(Long userId, Long amount) {
    Point point = pointRepository.findByUserId(userId)
        .orElseThrow();

    // ⚠️ 동시 사용 시 잔액 검증 우회 가능
    point.use(amount);
    pointRepository.save(point);
}
```

**시나리오**: 잔액 1000원인 사용자가 동시에 2번 결제(각 1000원)
- **예상 결과**: 1번 성공, 1번 실패
- **실제 결과**: 2번 모두 성공 (잔액 -1000원)

---

### 2.4 주문 생성 프로세스

#### 복합 동시성 문제
```java
// ❌ 동시성 제어 없는 코드
@Transactional
public Order createOrder(Long userId, List<OrderItem> items, Long couponId) {
    // 1. 재고 차감 (동시성 이슈)
    for (OrderItem item : items) {
        productService.decreaseStock(item.getProductId(), item.getQuantity());
    }

    // 2. 쿠폰 사용 (동시성 이슈)
    if (couponId != null) {
        couponService.useCoupon(userId, couponId);
    }

    // 3. 주문 생성
    return orderRepository.save(order);
}
```

**문제점**:
1. 재고 차감 중 다른 스레드가 동일 상품 주문 가능
2. 쿠폰 중복 사용 가능
3. 트랜잭션 롤백 시 재고/쿠폰 복구 누락

---

## 3. 해결 방안 선정

### 3.1 낙관적 락 vs 비관적 락 비교

| 항목 | 낙관적 락 (Optimistic Lock) | 비관적 락 (Pessimistic Lock) |
|------|---------------------------|----------------------------|
| **작동 원리** | Version 컬럼으로 충돌 감지 | SELECT FOR UPDATE로 행 잠금 |
| **충돌 가정** | 충돌이 드물다 | 충돌이 빈번하다 |
| **성능** | 높음 (락 없이 처리) | 낮음 (락 대기 발생) |
| **충돌 처리** | 재시도 필요 | 순차 대기 |
| **적용 적합성** | 읽기 중심, 충돌 적음 | 쓰기 중심, 충돌 빈번 |
| **사용자 경험** | 충돌 시 재시도 요청 | 대기 후 처리 |
| **데드락 위험** | 없음 | 있음 (여러 락 획득 시) |

### 3.2 도메인별 락 전략 선정

| 도메인 | 선택한 락 | 선정 이유 | 비고 |
|--------|---------|----------|-----|
| **포인트 충전** | 낙관적 락 | • 충돌 빈도 낮음 (사용자당 독립)<br>• 빠른 처리 우선 | 재시도 50회 |
| **쿠폰 발급** | 낙관적 락 | • 한시적 이벤트 (짧은 시간 고부하)<br>• 재시도로 충분히 처리 가능 | 재시도 5회 |
| **재고 차감** | 낙관적 락 | • 상품별 독립적 처리<br>• 읽기(조회) 빈도가 쓰기보다 높음 | 재시도 10회 |
| **주문 생성** | 낙관적 락 | • 재고/쿠폰과 동일한 전략 유지<br>• 일관성 유지 | 복합 트랜잭션 |

**비관적 락을 선택하지 않은 이유**:
1. **데드락 위험**: 주문 생성 시 여러 상품의 재고를 차감하는 경우, 락 획득 순서에 따라 데드락 발생 가능
2. **성능 저하**: 락 대기 시간으로 인한 응답 시간 증가
3. **확장성**: 트래픽 증가 시 락 경합으로 병목 현상 심화

**낙관적 락의 장점**:
1. **무락(lock-free) 읽기**: 조회 성능 우수
2. **데드락 없음**: Version 체크만으로 충돌 감지
3. **확장성**: 트래픽 증가에 유연하게 대응

---

### 3.3 선정 근거 및 트레이드오프

#### 낙관적 락 선택 이유

**1. 충돌 빈도 분석**
```
- 포인트 충전: 사용자별 독립적 → 충돌 거의 없음
- 쿠폰 발급: 이벤트 시간대에만 집중 → 일시적 충돌
- 재고 차감: 상품별 독립적 → 인기 상품만 충돌
```

**2. 성능 요구사항**
```
- 목표 TPS: 1000 이상
- 응답시간: P95 < 500ms
- 가용성: 99.9% 이상
```
→ 비관적 락의 대기 시간은 이 목표를 달성하기 어려움

**3. 사용자 경험**
```
- 낙관적 락: 빠른 응답 + 재시도
- 비관적 락: 느린 응답 (락 대기)
```
→ 빠른 실패 후 재시도가 더 나은 UX

#### 트레이드오프

**낙관적 락의 단점과 대응 방안**:

| 단점 | 대응 방안 |
|-----|---------|
| 충돌 시 재시도 필요 | Spring Retry로 자동 재시도 |
| 재시도 횟수 설정 어려움 | 도메인별 최적 횟수 테스트로 결정 |
| 무한 재시도 위험 | maxAttempts 제한 + 지수 백오프 |

---

## 4. 구현 계획

### 4.1 낙관적 락 적용 방법

#### 1단계: Entity에 @Version 추가
```java
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer stock;

    @Version  // ⭐ 낙관적 락
    private Long version;

    public void decreaseStock(Integer quantity) {
        if (this.stock < quantity) {
            throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
        }
        this.stock -= quantity;
    }
}
```

#### 2단계: Service에 @Retryable 적용
```java
@Service
@RequiredArgsConstructor
public class ProductService {

    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 10,
        backoff = @Backoff(delay = 50, maxDelay = 200, random = true)
    )
    @Transactional
    public void decreaseStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.decreaseStock(quantity);
        productRepository.save(product);
    }
}
```

#### 3단계: Application에 @EnableRetry 추가
```java
@EnableRetry
@SpringBootApplication
public class HhplusECommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(HhplusECommerceApplication.class, args);
    }
}
```

---

### 4.2 도메인별 구현 계획

#### 4.2.1 상품 재고 관리

**Entity**:
```java
@Entity
public class Product {
    @Version
    private Long version;

    public void decreaseStock(Integer quantity) {
        if (this.stock < quantity) {
            throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
        }
        this.stock -= quantity;
    }
}
```

**Service**:
```java
@Retryable(
    retryFor = {ObjectOptimisticLockingFailureException.class},
    maxAttempts = 10,
    backoff = @Backoff(delay = 50, maxDelay = 200, random = true)
)
@Transactional
public void decreaseStock(Long productId, Integer quantity) {
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

    product.decreaseStock(quantity);
    productRepository.save(product);
}
```

**재시도 전략**:
- maxAttempts: 10회 (일반 상품 동시성 수준)
- backoff: 50-200ms 랜덤 (짧은 간격으로 빠른 재시도)

---

#### 4.2.2 선착순 쿠폰 발급

**Entity**:
```java
@Entity
public class Coupon {
    @Version
    private Long version;

    public boolean canIssue() {
        return this.issuedQuantity < this.totalQuantity;
    }

    public void increaseIssuedQuantity() {
        if (!canIssue()) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }
        this.issuedQuantity++;
    }
}
```

**Service**:
```java
@Retryable(
    retryFor = {ObjectOptimisticLockingFailureException.class},
    maxAttempts = 5,
    backoff = @Backoff(delay = 50)
)
@Transactional
public UserCoupon issueCoupon(Long userId, Long couponId) {
    Coupon coupon = couponRepository.findById(couponId)
        .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

    // 중복 발급 확인
    userCouponRepository.findByUserIdAndCouponId(userId, couponId)
        .ifPresent(uc -> {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        });

    if (!coupon.canIssue()) {
        throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
    }

    coupon.increaseIssuedQuantity();
    couponRepository.save(coupon);

    UserCoupon userCoupon = UserCoupon.issue(userId, coupon);
    return userCouponRepository.save(userCoupon);
}
```

**재시도 전략**:
- maxAttempts: 5회 (쿠폰은 소진되면 빠른 실패가 나음)
- backoff: 50ms 고정 (예측 가능한 재시도)

---

#### 4.2.3 포인트 충전/사용

**Entity**:
```java
@Entity
public class Point {
    @Version
    private Long version;

    public void charge(Long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_POINT_AMOUNT);
        }
        this.amount += amount;
    }

    public void use(Long amount) {
        if (this.amount < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }
        this.amount -= amount;
    }
}
```

**Service (충전)**:
```java
@Retryable(
    retryFor = {ObjectOptimisticLockingFailureException.class, OptimisticLockException.class},
    maxAttempts = 50,
    backoff = @Backoff(delay = 1, maxDelay = 10, random = true)
)
@Transactional
public Point chargePoint(Long userId, Long amount) {
    Point point = pointRepository.findByUserId(userId)
        .orElseGet(() -> pointRepository.save(Point.create(userId)));

    point.charge(amount);
    Point savedPoint = pointRepository.save(point);

    // 포인트 거래 내역 저장
    PointTransaction transaction = PointTransaction.create(
        savedPoint.getId(), amount, TransactionType.CHARGE, savedPoint.getAmount()
    );
    pointTransactionRepository.save(transaction);

    return savedPoint;
}
```

**Service (사용)**:
```java
@Retryable(
    retryFor = {ObjectOptimisticLockingFailureException.class, OptimisticLockException.class},
    maxAttempts = 50,
    backoff = @Backoff(delay = 1, maxDelay = 10, random = true)
)
@Transactional
public Point usePoint(Long userId, Long amount) {
    Point point = pointRepository.findByUserId(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.POINT_NOT_FOUND));

    point.use(amount);
    Point savedPoint = pointRepository.save(point);

    PointTransaction transaction = PointTransaction.create(
        savedPoint.getId(), amount, TransactionType.USE, savedPoint.getAmount()
    );
    pointTransactionRepository.save(transaction);

    return savedPoint;
}
```

**재시도 전략**:
- maxAttempts: 50회 (포인트는 반드시 처리되어야 함)
- backoff: 1-10ms 랜덤 (매우 짧은 간격으로 빠른 재시도)

---

### 4.3 통합 테스트 계획

#### 테스트 시나리오

| 테스트 케이스 | 동시 스레드 | 초기 상태 | 예상 결과 | 검증 항목 |
|-------------|-----------|---------|---------|---------|
| 포인트 충전 | 100 | 잔액 0원 | 충전 100회 성공, 최종 100,000원 | 충전 누락 없음 |
| 포인트 사용 | 100 | 잔액 50,000원 | 사용 50회 성공, 50회 실패 | 초과 사용 방지 |
| 쿠폰 발급 | 1000 | 쿠폰 100개 | 발급 100개, 실패 900개 | 초과 발급 방지 |
| 재고 차감 | 100 | 재고 50개 | 판매 50개, 실패 50개 | 음수 재고 방지 |
| 주문 생성 | 100 | 상품3종, 각 20개 | 주문 20개 성공 | 재고/포인트 정합성 |

#### 테스트 코드 예시
```java
@SpringBootTest
@Testcontainers
class ConcurrencyTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb");

    @Test
    @DisplayName("100명이 동시에 포인트 충전 시 모두 성공해야 한다")
    void concurrentPointCharge() throws InterruptedException {
        // Given
        Long userId = 1L;
        Long chargeAmount = 1000L;
        int threadCount = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId, chargeAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        Point point = pointRepository.findByUserId(userId).orElseThrow();

        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(point.getAmount()).isEqualTo(100_000L);
    }
}
```

---

## 5. 결론

### 5.1 핵심 결정 사항

1. **낙관적 락 채택**: 성능과 확장성을 고려한 선택
2. **Spring Retry 활용**: 재시도 로직 자동화로 코드 간결화
3. **도메인별 차별화**: 각 도메인의 특성에 맞는 재시도 전략 적용
4. **통합 테스트 필수**: Testcontainers로 실제 MySQL 환경 재현

### 5.2 기대 효과

| 항목 | Before | After | 개선율 |
|-----|--------|-------|-------|
| **데이터 정합성** | 동시성 문제 발생 | 100% 보장 | ✅ |
| **코드 복잡도** | 수동 락 관리 | 선언적 처리 | -60% |
| **재시도 로직** | 각 Service 중복 | Spring Retry로 통일 | -70% |
| **테스트 가능성** | 어려움 | Testcontainers로 검증 | ✅ |

### 5.3 향후 고려 사항

**확장 시나리오**:
1. **분산 환경 전환**: Redis 분산 락 도입 시
2. **초고부하 이벤트**: 메시지 큐(Kafka) 기반 비동기 처리
3. **글로벌 서비스**: 멀티 리전 데이터 일관성 (Saga 패턴)

