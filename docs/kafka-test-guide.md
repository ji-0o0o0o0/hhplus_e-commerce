# Kafka 테스트 가이드

## 사전 준비

### 1. 카프카 실행 확인

```bash
# 카프카 컨테이너 확인
docker ps | findstr kafka

# 예상 출력:
# kafka        Up X minutes   0.0.0.0:9092->9092/tcp
# zookeeper    Up X minutes   0.0.0.0:2181->2181/tcp
# kafka-ui     Up X minutes   0.0.0.0:8989->8080/tcp
```

카프카가 실행되지 않았다면:
```bash
docker-compose -f docker-compose.kafka.yml up -d
```

### 2. 카프카 UI 접속

브라우저에서 http://localhost:8989 접속
- Topics 메뉴에서 `payment-completed` 토픽 확인
- 파티션 6개 확인

### 3. 애플리케이션 실행

MySQL, Redis가 실행 중이어야 합니다.

```bash
# MySQL, Redis 실행 확인
docker ps

# 애플리케이션 실행
./gradlew bootRun
```

---

## 테스트 시나리오

### 시나리오 1: 결제 완료 → 카프카 메시지 발행

#### 1단계: 초기 데이터 준비

먼저 주문을 생성해야 합니다.

```bash
# 1. 포인트 충전 (userId=1, 100,000원)
POST http://localhost:8080/api/points/1/charge
Content-Type: application/json

{
  "amount": 100000
}

# 2. 상품 조회 (재고 확인)
GET http://localhost:8080/api/products

# 응답 예시:
# {
#   "productId": 1,
#   "name": "노트북",
#   "price": 50000,
#   "stockQuantity": 100
# }

# 3. 주문 생성
POST http://localhost:8080/api/orders
Content-Type: application/json

{
  "userId": 1,
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ],
  "couponId": null
}

# 응답에서 orderId 확인 (예: 1)
```

#### 2단계: 결제 실행

```bash
POST http://localhost:8080/api/payments/1
Content-Type: application/json

{
  "userId": 1
}
```

#### 3단계: 로그 확인

애플리케이션 콘솔에서 다음 로그 확인:

```
[이벤트] 결제 완료 이벤트 수신 - OrderId: 1
[Kafka Producer] 결제 완료 메시지 발행 시작 - Topic: payment-completed, Key: 1, OrderId: 1
[Kafka Producer] 결제 완료 메시지 발행 성공 - Topic: payment-completed, Partition: 2, Offset: 0, OrderId: 1
```

**확인 포인트:**
- `Key: 1` → userId
- `Partition: 2` → userId의 해시 값에 따라 결정된 파티션
- `Offset: 0` → 해당 파티션의 첫 메시지

#### 4단계: Consumer 로그 확인

```
[Kafka Consumer] 결제 완료 메시지 수신 - Partition: 2, Offset: 0, OrderId: 1, UserId: 1
[데이터 플랫폼] 주문 정보 전송 시작 - OrderId: 1
[데이터 플랫폼] 전송 완료 - OrderId: 1, UserId: 1, FinalAmount: 100000, Status: COMPLETED
[Kafka Consumer] 결제 완료 메시지 처리 완료 - Partition: 2, Offset: 0, OrderId: 1
```

**확인 포인트:**
- Producer와 Consumer의 Partition, Offset이 일치
- 데이터 플랫폼 전송 완료 (1초 지연 시뮬레이션)
- 메시지 처리 완료 후 커밋

#### 5단계: Kafka UI에서 확인

http://localhost:8989 접속
1. Topics → `payment-completed` 클릭
2. Messages 탭 클릭
3. 발행된 메시지 확인:

```json
{
  "orderId": 1,
  "userId": 1,
  "totalAmount": 100000,
  "discountAmount": 0,
  "finalAmount": 100000,
  "couponId": null,
  "items": [
    {
      "productId": 1,
      "productName": "노트북",
      "quantity": 2,
      "unitPrice": 50000
    }
  ],
  "completedAt": "2025-12-18T23:00:00"
}
```

---

### 시나리오 2: 같은 사용자의 순서 보장 확인

같은 userId로 여러 번 결제하면 같은 파티션에 저장되는지 확인

#### 테스트 절차:

```bash
# userId=1로 3번 결제 실행
POST http://localhost:8080/api/payments/{orderId1} (userId=1)
POST http://localhost:8080/api/payments/{orderId2} (userId=1)
POST http://localhost:8080/api/payments/{orderId3} (userId=1)
```

#### 예상 로그:

```
[Kafka Producer] Partition: 2, Offset: 0, OrderId: 1  (userId=1)
[Kafka Producer] Partition: 2, Offset: 1, OrderId: 2  (userId=1)
[Kafka Producer] Partition: 2, Offset: 2, OrderId: 3  (userId=1)
```

**확인 포인트:**
- 모두 **같은 파티션(2)**에 저장
- Offset이 순차적으로 증가 (0, 1, 2)
- 같은 사용자의 메시지는 순서 보장

---

### 시나리오 3: 다른 사용자의 병렬 처리 확인

다른 userId로 결제하면 다른 파티션에 분산되는지 확인

#### 테스트 절차:

```bash
# userId=1, 2, 3으로 각각 결제
POST http://localhost:8080/api/payments/{orderId1} (userId=1)
POST http://localhost:8080/api/payments/{orderId2} (userId=2)
POST http://localhost:8080/api/payments/{orderId3} (userId=3)
```

#### 예상 로그:

```
[Kafka Producer] Partition: 2, Offset: 0, OrderId: 1  (userId=1)
[Kafka Producer] Partition: 5, Offset: 0, OrderId: 2  (userId=2)
[Kafka Producer] Partition: 1, Offset: 0, OrderId: 3  (userId=3)
```

**확인 포인트:**
- **다른 파티션**에 저장 (2, 5, 1)
- 각 파티션의 Offset은 0부터 시작
- 파티션이 다르므로 병렬 처리 가능

---

### 시나리오 4: Consumer 장애 복구 테스트

Consumer가 중단되었다가 재시작해도 메시지를 놓치지 않는지 확인

#### 테스트 절차:

1. **애플리케이션 중지** (Ctrl+C 또는 Stop)

2. **카프카에 메시지 발행** (CLI로 직접 발행)

```bash
# Kafka 컨테이너 접속
docker exec -it kafka bash

# Producer 실행
kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic payment-completed \
  --property "parse.key=true" \
  --property "key.separator=:"

# 메시지 입력 (JSON 형식)
1:{"orderId":999,"userId":1,"totalAmount":50000,"discountAmount":0,"finalAmount":50000,"couponId":null,"items":[{"productId":1,"productName":"테스트상품","quantity":1,"unitPrice":50000}],"completedAt":"2025-12-18T23:00:00"}
```

3. **애플리케이션 재시작**

```bash
./gradlew bootRun
```

4. **로그 확인**

```
[Kafka Consumer] 결제 완료 메시지 수신 - OrderId: 999, UserId: 1
[데이터 플랫폼] 전송 완료 - OrderId: 999
```

**확인 포인트:**
- 애플리케이션이 중지된 동안의 메시지도 처리됨
- Kafka가 메시지를 보관하고 있다가 Consumer 재시작 시 전달

---

### 시나리오 5: Consumer Lag 확인

메시지 발행 속도 > 처리 속도일 때 Lag 확인

#### Kafka UI에서 확인:

1. http://localhost:8989 접속
2. Consumers → `data-platform-service` 클릭
3. Consumer Lag 확인:

```
Topic: payment-completed
Partition 0: Lag 0 (Current Offset: 5, End Offset: 5)
Partition 1: Lag 0 (Current Offset: 3, End Offset: 3)
Partition 2: Lag 2 (Current Offset: 8, End Offset: 10) ← 2개 밀림
...
```

**Lag이 계속 증가한다면?**
- Consumer 처리 속도가 느림
- Consumer 수를 늘리거나 처리 로직 최적화 필요

---

## 문제 해결

### 1. 카프카 메시지가 발행되지 않음

**증상:**
```
[Kafka Producer] 결제 완료 메시지 발행 실패
```

**해결:**
1. 카프카 실행 확인: `docker ps | findstr kafka`
2. 포트 확인: `netstat -ano | findstr :9092`
3. 카프카 재시작:
   ```bash
   docker-compose -f docker-compose.kafka.yml restart kafka
   ```

### 2. Consumer가 메시지를 소비하지 않음

**증상:**
- Producer 로그는 있지만 Consumer 로그 없음

**해결:**
1. Consumer Group 확인:
   ```bash
   docker exec -it kafka bash
   kafka-consumer-groups --bootstrap-server localhost:9092 --list
   ```

2. Consumer Group 상세 확인:
   ```bash
   kafka-consumer-groups \
     --bootstrap-server localhost:9092 \
     --describe \
     --group data-platform-service
   ```

3. Offset 재설정 (처음부터 다시 읽기):
   ```bash
   kafka-consumer-groups \
     --bootstrap-server localhost:9092 \
     --group data-platform-service \
     --topic payment-completed \
     --reset-offsets \
     --to-earliest \
     --execute
   ```

### 3. 토픽이 생성되지 않음

**증상:**
- 애플리케이션 시작 시 토픽이 없다는 에러

**해결:**
```bash
# Kafka 컨테이너 접속
docker exec -it kafka bash

# 토픽 생성
kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic payment-completed \
  --partitions 6 \
  --replication-factor 1
```

---

## 유용한 명령어

### Kafka CLI 명령어

```bash
# Kafka 컨테이너 접속
docker exec -it kafka bash

# 토픽 목록
kafka-topics --list --bootstrap-server localhost:9092

# 토픽 상세 정보
kafka-topics --describe \
  --bootstrap-server localhost:9092 \
  --topic payment-completed

# Consumer Group 목록
kafka-consumer-groups --list --bootstrap-server localhost:9092

# Consumer Group Lag 확인
kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group data-platform-service

# 메시지 소비 (CLI)
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-completed \
  --from-beginning \
  --property print.key=true
```

---

## 다음 단계

STEP 17 완료 후:
- [ ] 쿠폰 발급 프로세스를 카프카로 개선 (STEP 18)
- [ ] 대기열 시스템을 카프카 Consumer 기반으로 변경
- [ ] DLT(Dead Letter Topic) 구현
- [ ] 멱등성 처리 개선 (중복 메시지 방지)
