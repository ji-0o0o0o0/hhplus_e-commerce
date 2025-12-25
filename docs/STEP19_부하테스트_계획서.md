# STEP 19 - 부하 테스트 계획서

## 📋 목차
1. [부하 테스트 개요](#1-부하-테스트-개요)
2. [시스템 현황 분석](#2-시스템-현황-분석)
3. [테스트 대상 및 시나리오](#3-테스트-대상-및-시나리오)
4. [테스트 환경 구성](#4-테스트-환경-구성)
5. [성능 목표 및 측정 지표](#5-성능-목표-및-측정-지표)
6. [실행 계획](#6-실행-계획)

---

## 1. 부하 테스트 개요

### 1.1 목적
이커머스 시스템의 안정성과 성능을 검증하고, 프로덕션 배포 전 다음 사항을 점검한다:

1. **예상 TPS 처리 가능 여부**
2. **평균/중간/최대 응답시간** 측정
3. **동시성 이슈 발생 여부** (재고 차감, 포인트 충전, 쿠폰 발급)
4. **병목 지점 식별** (DB, Redis, Kafka, Application)
5. **적절한 배포 스펙 산정**

### 1.2 테스트 유형

| 테스트 유형 | 설명 | 목적 |
|-----------|------|------|
| **Load Test** | 예상 부하 하에서 안정성 검증 | 목표 TPS 달성 여부, 적정 운영 스펙 파악 |
| **Peak Test** | 순간적인 최대 부하 처리 검증 | 선착순 이벤트 대응 능력 점검 |
| **Stress Test** | 점진적 부하 증가로 한계점 탐색 | 시스템 확장성 및 장기 운영 계획 수립 |

---

## 2. 시스템 현황 분석

### 2.1 주요 API 엔드포인트

#### 높은 우선순위 (동시성 제어 필수)

| API | Method | Endpoint | 동시성 제어 방식 | 비고 |
|-----|--------|----------|----------------|------|
| 포인트 충전 | POST | `/api/points/charge` | 분산 락 (Redisson) | 5초 대기, 10초 유지 |
| 주문 생성 | POST | `/api/orders` | 분산 락 (재고 차감) | 재고 동시성 이슈 |
| 쿠폰 발급 (동기) | POST | `/api/coupons/{id}/issue` | 분산 락 | 재고 동시성 이슈 |
| 쿠폰 발급 (Kafka) | POST | `/api/coupons/{id}/issue-kafka` | Kafka 비동기 | Consumer lag 확인 |

#### 중간 우선순위 (조회 성능)

| API | Method | Endpoint | 캐싱 전략 | 비고 |
|-----|--------|----------|----------|------|
| 인기 상품 조회 | GET | `/api/products/popular` | Redis 캐시 | 성능 비교 대상 |
| 인기 상품 조회 (DB) | GET | `/api/products/popular/db` | 없음 | 성능 비교 기준 |
| 상품 목록 조회 | GET | `/api/products` | 페이징 | Slow Query 가능성 |
| 상품 상세 조회 | GET | `/api/products/{id}` | Redis 캐시 | - |

### 2.2 동시성 제어 현황

#### 분산 락 (Redisson) - 현재 프로덕션 사용
```java
// 설정: waitTime 5초, leaseTime 10초
lockManager.executeWithLock(lockKey, 5L, 10L, () -> task)
```

**적용 대상:**
- 포인트 충전/사용: `chargePointWithDistributedLock`
- 쿠폰 발급: `issueCouponWithDistributedLock`
- 재고 차감: `decreaseProductStockWithDistributedLock`

#### 낙관적 락 (@Version) - 성능 비교용
```java
@Retryable(
    retryFor = {OptimisticLockException.class},
    maxAttempts = 5-10,
    backoff = @Backoff(delay = 50, maxDelay = 200)
)
```

**적용 엔티티:**
- Product (재고 차감)
- Point (잔액 변경)
- Coupon (발급 수량 증가)

#### Kafka 비동기 처리
- 쿠폰 발급: Producer → Consumer
- 멱등성: requestId 기반
- **측정 지표**: Consumer Lag, 처리 시간

### 2.3 예상 병목 지점

#### 1) 데이터베이스
```sql
-- 인기 상품 조회 (복잡한 쿼리)
SELECT ps.productId, SUM(ps.salesCount) as totalSales
FROM ProductStatistics ps
WHERE ps.statsDate >= :startDate
GROUP BY ps.productId
ORDER BY totalSales DESC
LIMIT :limit
```
- **예상 이슈**: statsDate 인덱스 부재 시 Full Scan
- **검증 방법**: EXPLAIN 분석, Slow Query 로그

#### 2) 동시성 제어
- **분산 락 대기 시간**: 많은 요청이 동시에 들어올 경우 락 획득 대기
- **낙관적 락 재시도**: 충돌 발생 시 재시도 오버헤드
- **DB 커넥션 풀 고갈**: 대기 중인 트랜잭션으로 인한 리소스 부족

#### 3) Redis
- **커넥션 풀 부족**: 동시 접속 증가 시
- **메모리 부족**: 캐시 데이터 증가

#### 4) Kafka
- **Consumer Lag**: 메시지 처리 속도 < 발행 속도
- **Partition 수**: 처리량 제한

---

## 3. 테스트 대상 및 시나리오

### 3.1 시나리오 1: 선착순 쿠폰 발급 (Peak Test)

#### 목적
- 동시 접속 폭주 상황에서의 시스템 안정성 검증
- 분산 락 vs Kafka 비동기 처리 성능 비교
- Consumer Lag 모니터링

#### 시나리오 상세
```
상황: 선착순 1000명 한정 쿠폰 이벤트
목표 TPS: 100-300 (초당)
테스트 시간: 1분
총 요청 수: 6,000-18,000건
```

#### 테스트 케이스

| 케이스 | API | VU (가상 사용자) | Duration | 예상 결과 |
|-------|-----|-----------------|----------|----------|
| TC-1 | `/api/coupons/{id}/issue` (분산 락) | 100 | 30s | TPS 50-100, 락 대기 발생 |
| TC-2 | `/api/coupons/{id}/issue-kafka` | 300 | 30s | TPS 200-300, Consumer Lag 발생 |
| TC-3 | 동시 호출 (분산 락 + Kafka) | 200 | 1m | 동시성 정합성 검증 |

#### 검증 포인트
- ✅ 쿠폰 재고 정합성 (발급 수량 = totalQuantity)
- ✅ 중복 발급 방지 (userId + couponId 유니크)
- ✅ Consumer Lag 최대값
- ✅ 평균 응답 시간 (p50, p95, p99)

---

### 3.2 시나리오 2: 상품 주문 폭주 (Load Test)

#### 목적
- 재고 차감 동시성 검증
- 포인트 차감 정합성 확인
- DB 트랜잭션 성능 측정

#### 시나리오 상세
```
상황: 인기 상품 주문 폭주 (재고 100개)
목표 TPS: 50-150
테스트 시간: 2분
총 요청 수: 6,000-18,000건
```

#### 테스트 케이스

| 케이스 | API | VU | Duration | 예상 결과 |
|-------|-----|-----|----------|----------|
| TC-1 | `/api/orders` (낙관적 락) | 50 | 2m | 재시도 발생, 재고 정합성 OK |
| TC-2 | `/api/orders` (분산 락) | 100 | 2m | TPS 80-120, 락 대기 발생 |
| TC-3 | 쿠폰 적용 주문 | 50 | 2m | 쿠폰 + 재고 동시 처리 검증 |

#### 검증 포인트
- ✅ 재고 정합성 (판매량 = 주문 수량 합)
- ✅ 포인트 잔액 정합성
- ✅ 오버셀링 방지 (재고 < 0 불가)
- ✅ DB 커넥션 풀 사용률

---

### 3.3 시나리오 3: 복합 트래픽 (Stress Test)

#### 목적
- 다양한 API 동시 호출 시 영향도 분석
- 특정 API의 부하가 다른 API에 미치는 영향 확인
- 시스템 전체 한계점 파악

#### 시나리오 상세
```
상황: 쿠폰 발급 이벤트 + 일반 쇼핑 트래픽
목표: 쿠폰 발급 중 상품 조회 속도 저하 여부
테스트 시간: 5분
```

#### 워크로드 구성

| API | 비중 | VU | 예상 TPS |
|-----|------|-----|---------|
| 쿠폰 발급 (Kafka) | 30% | 100 | 100 |
| 상품 조회 | 50% | 200 | 200 |
| 주문 생성 | 15% | 30 | 30 |
| 포인트 충전 | 5% | 10 | 10 |

#### 검증 포인트
- ✅ 각 API 응답 시간 변화 추이
- ✅ Redis 커넥션 풀 부족 여부
- ✅ DB 커넥션 풀 고갈 여부
- ✅ CPU/Memory 사용률

---

### 3.4 시나리오 4: 인덱스 제거 Before/After (성능 개선 검증)

#### 목적
- 인덱스 유무에 따른 성능 차이 측정
- Slow Query 발생 및 개선 과정 문서화

#### 테스트 절차
1. **Before**: 특정 인덱스 제거 → 부하 테스트 → Slow Query 로그 수집
2. **After**: 인덱스 재생성 → 동일 부하 테스트 → 성능 개선 확인

#### 대상 인덱스 후보

| 테이블 | 컬럼 | 쿼리 패턴 | 예상 효과 |
|-------|------|----------|----------|
| `product_statistics` | `statsDate` | `WHERE statsDate >= ?` | GROUP BY 성능 개선 |
| `orders` | `status, createdAt` | `WHERE status = ? AND createdAt < ?` | 결제 타임아웃 조회 개선 |
| `user_coupons` | `requestId` | `WHERE requestId = ?` | 비동기 발급 상태 조회 개선 |
| `user_coupons` | `userId, couponId` | `WHERE userId = ? AND couponId = ?` | 중복 발급 체크 개선 |

#### 측정 지표
- ✅ 쿼리 실행 시간 (Before vs After)
- ✅ EXPLAIN 분석 결과 (type: ALL → ref/range)
- ✅ 응답 시간 개선율 (%)

---

### 3.5 시나리오 5: 낙관적 락 vs 분산 락 성능 비교

#### 목적
- 두 락 방식의 TPS 및 응답 시간 비교
- 재시도 오버헤드 vs 락 대기 시간 분석

#### 비교 대상

| 기능 | 낙관적 락 API | 분산 락 API |
|------|--------------|------------|
| 포인트 충전 | `chargePoint` | `chargePointWithDistributedLock` |
| 쿠폰 발급 | `issueCoupon` | `issueCouponWithDistributedLock` |
| 주문 생성 | `createOrder` | `createOrderWithDistributedLock` |

#### 테스트 케이스

| VU | 낙관적 락 예상 TPS | 분산 락 예상 TPS | 비고 |
|----|------------------|-----------------|------|
| 10 | 80-100 | 50-70 | 충돌 적음, 낙관적 락 유리 |
| 50 | 100-150 | 80-120 | 충돌 증가, 재시도 발생 |
| 100 | 80-120 | 100-150 | 충돌 많음, 분산 락 유리 |

#### 검증 포인트
- ✅ TPS (Transactions Per Second)
- ✅ 평균/최대 응답 시간
- ✅ 에러율 (낙관적 락 재시도 실패)
- ✅ CPU 사용률

---

## 4. 테스트 환경 구성

### 4.1 애플리케이션 스펙

```yaml
환경: Local / Docker
CPU: 제한 없음 (점진적으로 제한하며 병목 탐색)
Memory: 제한 없음 (점진적으로 제한하며 병목 탐색)
JVM: -Xms512m -Xmx1024m (조정 가능)
```

**멘토링 포인트:**
- 처음부터 MAX로 설정하지 말고, 병목점을 찾으며 조금씩 늘리기
- CPU 10% 사용률을 기준으로 점진적 증가

### 4.2 데이터베이스

```yaml
DB: MySQL 8.0
Connection Pool: HikariCP
  - maximum-pool-size: 10 → 20 → 50 (점진적 증가)
  - minimum-idle: 5
  - connection-timeout: 30000ms
```

### 4.3 Redis

```yaml
Redis: 7.0
Connection Pool: Lettuce
  - max-active: 8
  - max-idle: 8
  - min-idle: 2
```

### 4.4 Kafka

```yaml
Kafka: 3.5.0
Partitions: 3 (쿠폰 발급 토픽)
Consumer:
  - concurrency: 3
  - max.poll.records: 100
```

### 4.5 부하 테스트 도구

```yaml
도구: k6 (https://k6.io/)
이유:
  - 경량 고성능
  - JavaScript 스크립트 작성
  - 풍부한 메트릭 제공
  - CLI 실행 간편

설치: brew install k6 (macOS) / choco install k6 (Windows)
```

**멘토링 포인트:**
- 부하 테스트 도구는 native 실행 권장 (컨테이너 불필요)
- k6, JMeter, nGrinder 등 팀 환경에 맞는 도구 선택

---

## 5. 성능 목표 및 측정 지표

### 5.1 목표 TPS

| API | 목표 TPS | 근거 |
|-----|---------|------|
| 쿠폰 발급 (Kafka) | 200-300 | 선착순 이벤트 대응 |
| 주문 생성 | 80-150 | 일반 주문 처리 |
| 포인트 충전 | 50-100 | 충전 빈도 낮음 |
| 상품 조회 | 500-1000 | 조회 중심 트래픽 |

**멘토링 포인트:**
- 동시성 문제가 명확한 기능은 초당 10-20건에도 문제 발생 가능
- 사용자 수, worker를 늘리며 최대 처리량 찾기

### 5.2 응답 시간 목표

| 지표 | 목표 | 설명 |
|------|------|------|
| p50 (중간값) | < 200ms | 50% 요청이 200ms 이내 응답 |
| p95 | < 500ms | 95% 요청이 500ms 이내 응답 |
| p99 | < 1000ms | 99% 요청이 1초 이내 응답 |
| 최대 응답 시간 | < 3000ms | 타임아웃 기준 |

### 5.3 에러율 목표

| 지표 | 목표 | 비고 |
|------|------|------|
| 에러율 | < 1% | 재고 소진 등 정상 에러 제외 |
| 타임아웃 | < 0.1% | 30초 기준 |

### 5.4 리소스 사용률 목표

| 리소스 | 목표 | 알림 기준 |
|--------|------|----------|
| CPU | < 70% | > 80% 경고 |
| Memory | < 80% | > 90% 경고 |
| DB Connection | < 80% | > 90% 경고 |
| Redis Connection | < 80% | > 90% 경고 |

### 5.5 Kafka 메트릭 목표

| 지표 | 목표 | 비고 |
|------|------|------|
| Consumer Lag | < 1000 | 실시간 처리 기준 |
| 처리 시간 | < 100ms | Consumer 로직 실행 시간 |

---

## 6. 실행 계획

### 6.1 일정

| 단계 | 작업 | 소요 시간 | 완료 기준 |
|------|------|----------|----------|
| 1 | 시스템 현황 파악 | 완료 | API, 동시성 제어, 인덱스 현황 문서화 |
| 2 | 테스트 계획 수립 | 완료 | 본 문서 작성 |
| 3 | k6 설치 및 환경 준비 | 30분 | k6 설치, 샘플 스크립트 실행 |
| 4 | 테스트 데이터 준비 | 1시간 | 사용자, 상품, 쿠폰 데이터 생성 |
| 5 | 테스트 스크립트 작성 | 2시간 | 5개 시나리오 스크립트 완성 |
| 6 | 부하 테스트 실행 | 3시간 | 시나리오별 테스트 실행 및 결과 수집 |
| 7 | 결과 분석 및 개선 | 2시간 | 병목 식별, 개선안 도출 |

### 6.2 테스트 실행 순서

```
1. Warm-up (준비 운동)
   - VU 10, Duration 1분
   - 애플리케이션 캐시 워밍, JIT 컴파일 완료

2. Load Test (기본 부하)
   - VU 50, Duration 2분
   - 목표 TPS 달성 여부 확인

3. Peak Test (최대 부하)
   - VU 100 → 300, Duration 1분
   - 순간 트래픽 폭증 대응 능력

4. Stress Test (한계 탐색)
   - VU 점진적 증가 (10 → 500), Duration 5분
   - 시스템 한계점 파악

5. 복합 시나리오
   - 여러 API 동시 호출
   - 실제 운영 환경 시뮬레이션
```

### 6.3 모니터링 항목

#### 애플리케이션 로그
```bash
# 에러 로그 모니터링
tail -f logs/application.log | grep ERROR

# Slow Query 로그
tail -f logs/slow-query.log
```

#### 시스템 메트릭
- CPU, Memory, Disk I/O
- JVM Heap 사용량
- GC 빈도 및 소요 시간

#### DB 메트릭
```sql
-- 실행 중인 쿼리 확인
SHOW FULL PROCESSLIST;

-- 느린 쿼리 확인
SELECT * FROM mysql.slow_log ORDER BY query_time DESC LIMIT 10;

-- 커넥션 수 확인
SHOW STATUS LIKE 'Threads_connected';
SHOW VARIABLES LIKE 'max_connections';
```

#### Redis 메트릭
```bash
# Redis 상태 확인
redis-cli INFO stats
redis-cli INFO clients

# 커넥션 수 확인
redis-cli CLIENT LIST | wc -l
```

#### Kafka 메트릭
```bash
# Consumer Lag 확인
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group coupon-consumer-group --describe

# 토픽 상태 확인
kafka-topics --bootstrap-server localhost:9092 --describe --topic coupon-issue-requests
```

### 6.4 결과 수집 항목

#### k6 메트릭
- `http_reqs`: 총 요청 수
- `http_req_duration`: 응답 시간 (p50, p95, p99, max)
- `http_req_failed`: 실패율
- `vus`: 가상 사용자 수
- `iterations`: 반복 횟수

#### 데이터 정합성
```sql
-- 쿠폰 발급 정합성
SELECT couponId, COUNT(*) as issued_count
FROM user_coupons
GROUP BY couponId;

SELECT id, totalQuantity, issuedQuantity
FROM coupons;

-- 재고 정합성
SELECT p.id, p.stock,
       (SELECT SUM(quantity) FROM order_items WHERE productId = p.id) as sold
FROM products p;

-- 포인트 정합성
SELECT userId, amount,
       (SELECT SUM(amount) FROM point_transactions WHERE pointId = p.id AND type = 'CHARGE') as total_charge,
       (SELECT SUM(amount) FROM point_transactions WHERE pointId = p.id AND type = 'USE') as total_use
FROM points p;
```

---

## 7. 예상 문제 및 대응 방안

### 7.1 분산 락 대기 시간 초과

**증상:**
```
LockAcquisitionException: 락 획득에 실패했습니다
```

**원인:**
- 많은 요청이 동시에 락 획득 대기
- waitTime (5초) 내에 락 획득 실패

**대응 방안:**
1. waitTime 증가 (5초 → 10초)
2. leaseTime 감소 (10초 → 5초)
3. Kafka 비동기 처리로 전환

### 7.2 DB 커넥션 풀 고갈

**증상:**
```
HikariPool - Connection is not available
```

**원인:**
- 동시 트랜잭션 수 > 커넥션 풀 사이즈
- 락 대기로 인한 트랜잭션 장기화

**대응 방안:**
1. Connection Pool Size 증가 (10 → 20 → 50)
2. 트랜잭션 범위 최소화
3. Read-Only 쿼리는 별도 DataSource 사용

### 7.3 낙관적 락 재시도 실패

**증상:**
```
ObjectOptimisticLockingFailureException
```

**원인:**
- 충돌 빈도 > 재시도 횟수
- 높은 동시성 환경

**대응 방안:**
1. maxAttempts 증가 (5 → 10)
2. Backoff 시간 조정 (random = true)
3. 분산 락으로 전환

### 7.4 Kafka Consumer Lag 증가

**증상:**
```
Consumer Lag: 10,000+
```

**원인:**
- 메시지 발행 속도 > 소비 속도
- Consumer 로직 병목 (DB 쓰기)

**대응 방안:**
1. Consumer Concurrency 증가 (3 → 6)
2. Partition 수 증가 (3 → 6)
3. Batch Insert 처리

---

## 8. 다음 단계 (STEP 20)

부하 테스트 실행 후 다음 내용을 포함한 보고서 작성:

1. **테스트 결과 요약**
   - 시나리오별 TPS, 응답 시간, 에러율
   - 목표 달성 여부

2. **병목 지점 분석**
   - Slow Query 목록 및 실행 계획
   - 리소스 사용률 그래프
   - 에러 로그 분석

3. **개선 작업**
   - 인덱스 추가/변경
   - 쿼리 최적화
   - 설정 튜닝

4. **가상 장애 대응 문서**
   - 장애 시나리오 정의
   - 타임라인 작성
   - 대응 절차 및 결과
   - 개선 액션 아이템

