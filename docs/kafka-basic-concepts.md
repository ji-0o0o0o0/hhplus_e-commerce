# Kafka 기초 개념 학습

## 1. Kafka란?

카프카는 대규모 실시간 데이터 스트리밍을 위한 **분산 메시징 시스템**입니다.
- 높은 처리량(High Throughput)
- 고가용성(High Availability)
- 확장성(Scalability)
- 내구성(Durability)

### 왜 카프카를 사용하는가?

분산 시스템에서 서비스 간 비동기 통신이 필요할 때:
- 서비스 간 결합도 감소
- 장애 격리(Fault Isolation)
- 비즈니스 로직의 관심사 분리
- 메시지 재처리 가능

## 2. Kafka 핵심 구성 요소

### 2.1 Producer (생산자)
- 메시지를 카프카 브로커에 발행(Publish)하는 서비스
- 메시지를 어떤 토픽의 어떤 파티션에 보낼지 결정
- 예: 주문 서비스에서 "주문 완료" 메시지 발행

### 2.2 Consumer (소비자)
- 카프카 브로커에서 메시지를 읽어오는(Consume) 서비스
- 메시지를 읽을 때마다 **Offset**을 유지해 처리 위치를 추적
- `CURRENT-OFFSET`: 컨슈머가 어디까지 처리했는지 나타내는 위치
  - 동일한 메시지 재처리 방지
  - 처리하지 않은 메시지 건너뛰기 방지
- 오류 발생 시 `--reset-offsets` 옵션으로 특정 시점으로 되돌릴 수 있음

### 2.3 Broker (브로커)
- 카프카 서버의 기본 단위
- Producer의 메시지를 받아 Offset 지정 후 디스크에 저장
- Consumer의 파티션 Read 요청에 응답해 메시지 전송

**특수한 역할을 가진 Broker:**

1. **Controller**
   - 다른 브로커를 모니터링
   - 장애 발생한 브로커의 Leader 파티션을 다른 브로커로 재분배

2. **Coordinator**
   - 컨슈머 그룹 모니터링
   - 특정 컨슈머 장애 시 파티션을 다른 컨슈머에게 재할당 (Rebalance)

### 2.4 Message (메시지)
- 카프카에서 취급하는 데이터의 단위
- `<Key, Message>` 형태로 구성
- Key를 통해 파티션 결정 및 순서 보장

### 2.5 Topic & Partition (토픽 & 파티션)

**Topic**
- 메시지를 분류하는 논리적 단위
- N개의 Partition으로 구성
- 예: `order-completed`, `payment-completed`

**Partition**
- 토픽 내의 물리적 분할 단위
- 발행된 순서대로 메시지를 저장하여 **순차 처리 보장**
- 대용량 트래픽을 **파티션 개수만큼 병렬 처리** 가능

**파티션 할당 방식:**
```
key: "지영"
hash: 50649483  // "지영".hashCode()
partitionCnt: 3

targetPartition: 1  // (50649483 % 3)
```

- **키가 있는 경우**: 키의 해시 값을 기준으로 파티션 결정 → 같은 키는 항상 같은 파티션
- **키가 없는 경우**: Round-Robin 방식으로 균등 분배

**중요한 특징:**
- 전체 메시지의 순서는 보장하지 않지만, **같은 파티션 내 메시지는 순차 처리 보장**
- 하나의 파티션은 하나의 컨슈머에서만 컨슘 가능 (동일 컨슈머 그룹 내)

**실용 예시:**
```
포인트 충전/차감 시나리오:
- 유저1, 유저2, 유저3의 포인트 작업은 동시에 처리 가능 (다른 파티션)
- 하지만 유저1의 충전과 차감은 순차적으로 처리되어야 함 (같은 파티션)
→ userId를 메시지 키로 설정
```

### 2.6 Consumer Group (컨슈머 그룹)

- 하나의 토픽에 발행된 메시지를 여러 서비스가 독립적으로 컨슘하기 위한 그룹
- 보통 Application 단위로 Consumer Group 생성

**예시:**
```
하나의 "주문 완료" 메시지를
- 결제 서비스도 컨슘
- 상품 서비스도 컨슘
- 알림 서비스도 컨슘
→ 각각 별도의 Consumer Group
```

**중요 규칙:**
- 파티션 수 ≥ 컨슈머 수 (동일 그룹 내)
- 컨슈머가 파티션보다 많으면 일부 컨슈머는 놀게 됨
- 파티션이 컨슈머보다 많으면 하나의 컨슈머가 여러 파티션 담당

### 2.7 Rebalancing (리밸런싱)

Consumer Group의 **가용성과 확장성**을 확보하는 메커니즘

**발생 시점:**
1. Consumer Group 내에 새로운 Consumer 추가
2. 특정 Consumer 장애로 소비 중단
3. Topic 내에 새로운 Partition 추가

**동작:**
- 특정 컨슈머의 파티션 소유권을 다른 컨슈머로 이전

**주의사항:**
- 리밸런싱 중에는 컨슈머가 메시지를 읽을 수 없음 (일시적 중단)

### 2.8 Cluster (클러스터)

- 고가용성(HA)을 위해 여러 Broker를 묶은 구성
- 브로커 증가 시 메시지 처리량 분산 가능
- 동작 중인 다른 브로커에 영향 없이 확장 가능

**최소 구성:**
- 최소 3대의 브로커 권장
- Replication Factor = 3 설정 시 2대까지 장애 허용

### 2.9 Replication (복제)

Cluster의 가용성을 보장하는 핵심 개념

**Leader Replica**
- 각 파티션당 1개 존재
- 모든 Producer/Consumer 요청은 Leader를 통해 처리
- 일관성 보장

**Follower Replica**
- Leader를 제외한 나머지 복제본
- Leader의 메시지를 지속적으로 복제하여 백업
- Leader 장애 시 Follower 중 하나가 새로운 Leader로 선출

**중요:**
- Leader의 메시지가 동기화되지 않은 Replica는 Leader로 선출될 수 없음

## 3. 메시지 전달 보장

### 3.1 At-Least-Once (최소 1회 전달)

Kafka의 기본 보장 수준:
- 메시지 유실 없음 ✓
- 중복 처리 가능성 있음 ⚠️

**장애 시나리오:**
1. 메시지 처리 완료
2. Offset 커밋 직전 컨슈머 장애
3. 재시작 시 같은 메시지 다시 처리 → 중복

### 3.2 멱등성(Idempotency) 설계

중복 처리를 방지하기 위한 애플리케이션 레벨 대응:

```java
public void processOrder(OrderEvent event) {
    // 이미 처리된 주문인지 확인
    if (orderRepository.existsById(event.getOrderId())) {
        log.info("이미 처리된 주문: {}", event.getOrderId());
        return;  // 중복 처리 방지
    }

    // 실제 처리
    orderRepository.save(event.toOrder());
}
```

**구현 방법:**
- INSERT 전 ID 존재 여부 체크
- DB Unique 제약 조건
- 처리 완료 상태 저장 및 체크

## 4. Offset 관리

### 4.1 자동 커밋 vs 수동 커밋

**자동 커밋의 문제점:**
```
메시지 10개 가져옴
↓
3개 처리 중 (5초 후 자동 커밋 발생!)
↓
장애 발생
↓
재시작 → 커밋된 위치부터 시작
↓
7개 처리 안 됨 (유실)
```

**수동 커밋 권장:**
```java
@KafkaListener(topics = "orders")
public void consume(ConsumerRecord<String, String> record,
                   Acknowledgment ack) {
    try {
        // 비즈니스 로직 처리
        processOrder(record.value());

        // 처리 완료 후 명시적 커밋
        ack.acknowledge();
    } catch (Exception e) {
        // 커밋하지 않음 → 재처리됨
    }
}
```

**장점:**
- 처리 완료 시점 = 커밋 시점 일치
- 유실 방지
- 정확한 제어

### 4.2 Ack Mode

- **RECORD**: 메시지 하나마다 커밋 (가장 안전, 오버헤드 있음)
- **BATCH**: 배치 단위 커밋 (성능과 안정성 균형)
- **MANUAL**: 개발자가 직접 acknowledge() 호출 (세밀한 제어)

## 5. 실무 적용 전략

### 5.1 비동기 메시지 통신을 통한 책임 분리

**Before (Application Event):**
```java
class OrderPaymentService {
    @Transactional
    fun 주문_결제() {
        유저_포인트_차감();
        결제_정보_저장();
        주문_상태_변경();
        결제_완료_이벤트_발행();
    }
}

class OrderPaymentEventListener {
    @TransactionalEventListener(AFTER_COMMIT)
    @Async
    fun 주문_정보_전달(결제_완료_이벤트) {
        // 데이터 수집 플랫폼 API 호출
        // 실패 시 재시도 로직 필요...
    }
}
```

**문제점:**
- 데이터 수집 플랫폼 장애 시 재전송 책임이 주문 서비스에 있음
- 외부 서비스 장애가 주문 서비스에 영향

**After (Kafka):**
```java
class OrderPaymentEventListener {
    @TransactionalEventListener(AFTER_COMMIT)
    fun 결제완료_이벤트_처리(결제_완료_이벤트) {
        kafkaProducer.publish(결제_완료_이벤트);
    }
}
```

**장점:**
- 주문 서비스는 카프카에 메시지만 발행하면 책임 종료
- 데이터 수집 플랫폼, 알림 서비스는 각자 메시지를 알아서 컨슘
- 외부 서비스 장애가 주문 서비스에 영향 없음 (장애 격리)
- 부가 로직에 대한 관심사 완전히 제거

### 5.2 파티션을 활용한 병렬 처리 및 순서 보장

**선착순 쿠폰 발급 예시:**
```
쿠폰A 발급 요청 1000건 → 파티션 1 (쿠폰A 전용)
쿠폰B 발급 요청 800건  → 파티션 2 (쿠폰B 전용)
쿠폰C 발급 요청 500건  → 파티션 3 (쿠폰C 전용)

메시지 키 = 쿠폰 ID
→ 같은 쿠폰의 모든 요청은 같은 파티션에 순차 저장
→ 별도의 lock 없이 동시성 제어 + 초과 발급 방지
```

**장점:**
- 같은 쿠폰은 순서 보장 (동시성 제어)
- 다른 쿠폰은 병렬 처리 (처리량 향상)
- Redis lock 없이 구현 가능

### 5.3 처리량 향상 전략

**처리량을 높이기 위한 방법:**
1. **Partition 수 증가** ⭐ 가장 효과적
   - 병렬 처리 증가
   - Consumer 수도 함께 증가 필요

2. **Consumer 수 증가**
   - Partition 수만큼만 효과 있음
   - Partition보다 많으면 일부 Consumer 놀게 됨

3. **Producer 수 증가**
   - 발행 속도는 빨라지지만 처리 속도는 변화 없음

## 6. 성능 측정 지표

### 6.1 주요 지표

**1. Throughput (처리량)**
- 발행 TPS: Producer 초당 발행 건수
- 처리 TPS: Consumer 초당 처리 건수

**2. Latency (지연 시간)**
- 평균 레이턴시
- P95, P99 레이턴시 (상위 5%, 1%)
- Redis: 1-2ms
- Kafka: 수십ms ~ 수초 (비즈니스 로직에 따라)

**3. Consumer Lag (컨슈머 랙)** ⭐ 가장 중요
```
랙 = Producer 발행 Offset - Consumer 처리 Offset

랙이 계속 증가 → 처리량 부족 신호
```

**4. 자원 사용률**
- CPU, 메모리, 디스크 I/O, 네트워크 I/O

**5. 에러율 및 재시도 횟수**

### 6.2 모니터링 도구

**부하 테스트:**
- k6
- Gatling
- JMeter
- nGrinder

**메트릭 수집/시각화:**
- Grafana + Prometheus
- Datadog

## 7. Redis vs Kafka 비교

| 구분 | Redis | Kafka |
|------|-------|-------|
| 응답 방식 | 동기식 (즉시 응답) | 비동기식 |
| 데이터 보관 | 메모리 기반 (휘발성) | 디스크 기반 (영구 저장) |
| 처리 속도 | 매우 빠름 (1-2ms) | 상대적으로 느림 (수십ms~) |
| 재처리 | 어려움 | 쉬움 (Offset 조정) |
| 주요 용도 | 즉시 응답, 분산 락 | 비동기 처리, 이벤트 스트리밍 |
| 적합 케이스 | 선착순 수량 체크 | 주문 완료 후 알림 발송 |

### 하이브리드 아키텍처 (실무 권장)

```
[사용자 요청]
    ↓
[Redis INCR로 수량 체크] ← 즉시 응답
    ↓
100 이하? → "발급 진행 중" 응답 + Kafka로 메시지 발행
100 초과? → "마감" 즉시 응답
    ↓
[Kafka Consumer]
    ↓
DB 저장, 외부 API 호출 등 (비동기)
```

**장점:**
- 사용자에게 빠른 응답 (Redis)
- 무거운 작업은 비동기 처리 (Kafka)
- 재처리 가능

## 8. 실패 메시지 처리 (DLT 전략)

### 8.1 재시도 전략

**일시적 오류 (재시도로 해결 가능):**
- 네트워크 타임아웃
- 외부 API 일시 장애
- DB 일시적 이슈

→ **지수 백오프 재시도**
```
1차: 1초 후
2차: 2초 후
3차: 4초 후
4차: 8초 후
5차: 16초 후
→ 실패 시 DLT로 전송
```

**영구 오류 (재시도 무의미):**
- 비즈니스 규칙 위반
- 코드 버그
- 잘못된 데이터 형식

→ **즉시 DLT로 전송**

### 8.2 DLT (Dead Letter Topic) 처리

```
[DLT에 메시지 쌓임]
       ↓
[Slack 알림 발송] → 개발자 인지
       ↓
[원인 분석]
       ↓
조치 선택:
1. 원본 토픽에 재발행
2. 수동 재처리
3. 영구 삭제
```

### 8.3 Spring Kafka 설정

```java
@KafkaListener(topics = "orders")
@RetryableTopic(
    attempts = "3",
    backoff = @Backoff(delay = 1000, multiplier = 2),
    dltStrategy = DltStrategy.FAIL_ON_ERROR
)
public void consume(OrderEvent event) {
    // 비즈니스 로직
}
```

## 9. 핵심 정리

### 9.1 반드시 기억할 것

1. **메시지 순서 보장**: 같은 파티션 내에서만 보장
2. **파티션 수 = 컨슈머 수**: 최적의 병렬 처리
3. **수동 커밋 사용**: 처리 완료 시점에 커밋
4. **멱등성 설계**: At-Least-Once + 중복 체크
5. **컨슈머 랙 모니터링**: 가장 중요한 성능 지표

### 9.2 설계 원칙

1. **파티션 수는 늘리기 쉽고 줄이기 어려움** → 보수적으로 시작
2. **브로커는 최소 3대**, RF=3, 처리량 20-30% 여유 확보
3. **모든 상태 변경마다 이벤트 발행 X** → 비즈니스 의미가 있는 시점에만
4. **순서 중요**: 파티션 3-6개, **처리량 중요**: 파티션 12-24개

### 9.3 실무 팁

1. Schema Registry보다 **공통 DTO 모듈** 권장 (SPOF 방지)
2. DB 레벨 최종 검증은 반드시 수행 (Defense in Depth)
3. 일시적 오류는 재시도, 영구 오류는 즉시 DLT
4. 컨슈머 랙 모니터링을 잘하면 면접에서 강점


