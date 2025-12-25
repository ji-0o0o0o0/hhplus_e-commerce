# Kafka 로컬 환경 구축 가이드

## 1. Docker Compose로 Kafka 실행

### 1.1 Kafka 실행

```bash
# Kafka, Zookeeper, Kafka UI 모두 실행
docker-compose -f docker-compose.kafka.yml up -d

# 로그 확인
docker-compose -f docker-compose.kafka.yml logs -f kafka
```

### 1.2 실행 확인

```bash
# 컨테이너 상태 확인
docker ps

# 예상 출력:
# CONTAINER ID   IMAGE                               PORTS
# xxxx          confluentinc/cp-kafka:7.5.0        0.0.0.0:9092->9092/tcp
# xxxx          confluentinc/cp-zookeeper:7.5.0    0.0.0.0:2181->2181/tcp
# xxxx          provectuslabs/kafka-ui:latest      0.0.0.0:8989->8080/tcp
```

### 1.3 Kafka UI 접속

브라우저에서 `http://localhost:8989` 접속
- 토픽 목록 확인
- 메시지 발행/조회
- 컨슈머 그룹 모니터링

## 2. Kafka CLI로 기본 기능 실습

### 2.1 Kafka 컨테이너 접속

```bash
docker exec -it kafka bash
```

### 2.2 토픽 생성

```bash
# 토픽 생성 (파티션 3개, Replication Factor 1)
kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic test-topic \
  --partitions 3 \
  --replication-factor 1

# 토픽 목록 확인
kafka-topics --list --bootstrap-server localhost:9092

# 토픽 상세 정보 확인
kafka-topics --describe \
  --bootstrap-server localhost:9092 \
  --topic test-topic
```

### 2.3 메시지 발행 (Producer)

```bash
# Producer 실행
kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic test-topic

# 메시지 입력 (Enter로 전송)
> Hello Kafka!
> This is a test message
> 안녕하세요 카프카!
```

**키가 있는 메시지 발행:**
```bash
kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic test-topic \
  --property "parse.key=true" \
  --property "key.separator=:"

# 메시지 입력 (키:값 형태)
> user1:주문 완료
> user1:결제 완료
> user2:주문 완료
```

### 2.4 메시지 소비 (Consumer)

```bash
# Consumer 실행 (처음부터 읽기)
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic test-topic \
  --from-beginning

# Consumer 실행 (키와 함께 읽기)
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic test-topic \
  --from-beginning \
  --property print.key=true \
  --property key.separator=":"
```

**Consumer Group으로 실행:**
```bash
# Consumer Group 1
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic test-topic \
  --group consumer-group-1 \
  --from-beginning

# 다른 터미널에서 Consumer Group 1의 다른 Consumer
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic test-topic \
  --group consumer-group-1

# Consumer Group 2 (독립적으로 메시지 소비)
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic test-topic \
  --group consumer-group-2 \
  --from-beginning
```

### 2.5 Consumer Group 확인

```bash
# Consumer Group 목록
kafka-consumer-groups --list --bootstrap-server localhost:9092

# Consumer Group 상세 정보 (Lag 확인 가능)
kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group consumer-group-1

# 예상 출력:
# GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
# consumer-group-1 test-topic     0          5               5               0
# consumer-group-1 test-topic     1          3               3               0
# consumer-group-1 test-topic     2          4               4               0
```

**Lag 해석:**
- `CURRENT-OFFSET`: 컨슈머가 처리한 마지막 오프셋
- `LOG-END-OFFSET`: 파티션의 최신 오프셋
- `LAG`: 미처리 메시지 수 (LOG-END-OFFSET - CURRENT-OFFSET)
- LAG이 계속 증가하면 처리량 부족 신호!

### 2.6 Offset 재설정

```bash
# 특정 오프셋으로 재설정
kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group consumer-group-1 \
  --topic test-topic \
  --reset-offsets \
  --to-offset 5 \
  --execute

# 처음부터 다시 읽기
kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group consumer-group-1 \
  --topic test-topic \
  --reset-offsets \
  --to-earliest \
  --execute

# 최신 오프셋으로 이동
kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group consumer-group-1 \
  --topic test-topic \
  --reset-offsets \
  --to-latest \
  --execute
```

## 3. 실습 예제: 주문 완료 시나리오

### 3.1 토픽 생성

```bash
# 주문 완료 이벤트 토픽 (파티션 6개)
kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic order-completed \
  --partitions 6 \
  --replication-factor 1
```

### 3.2 Producer로 주문 이벤트 발행

```bash
kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic order-completed \
  --property "parse.key=true" \
  --property "key.separator=:"

# 메시지 발행 (userId를 키로 사용)
> user1:{"orderId":1001,"userId":"user1","amount":50000}
> user2:{"orderId":1002,"userId":"user2","amount":30000}
> user1:{"orderId":1003,"userId":"user1","amount":20000}
```

### 3.3 Consumer Group으로 병렬 처리

**터미널 1: 데이터 플랫폼 Consumer**
```bash
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-completed \
  --group data-platform-service \
  --property print.key=true
```

**터미널 2: 알림 서비스 Consumer**
```bash
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-completed \
  --group notification-service \
  --property print.key=true
```

**결과:**
- 두 서비스가 독립적으로 같은 메시지를 소비
- 각 서비스는 자신의 Offset 관리

## 4. 파티션과 순서 보장 실습

### 4.1 메시지 키와 파티션 할당 확인

```bash
# 파티션 정보와 함께 메시지 소비
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-completed \
  --from-beginning \
  --property print.key=true \
  --property print.partition=true

# 예상 출력:
# Partition:1  Key:user1  Value:{"orderId":1001,...}
# Partition:1  Key:user1  Value:{"orderId":1003,...}
# Partition:3  Key:user2  Value:{"orderId":1002,...}
```

**확인 사항:**
- 같은 키(user1)의 메시지가 같은 파티션(1)에 저장됨
- 파티션 내에서는 순서가 보장됨

## 5. Kafka 종료 및 정리

### 5.1 Kafka 종료

```bash
# Kafka 컨테이너 종료
docker-compose -f docker-compose.kafka.yml down

# 데이터까지 모두 삭제
docker-compose -f docker-compose.kafka.yml down -v
```

### 5.2 특정 토픽 삭제

```bash
kafka-topics --delete \
  --bootstrap-server localhost:9092 \
  --topic test-topic
```

## 6. 문제 해결

### 6.1 Kafka 컨테이너가 시작되지 않는 경우

```bash
# 로그 확인
docker-compose -f docker-compose.kafka.yml logs kafka

# 기존 볼륨 삭제 후 재시작
docker-compose -f docker-compose.kafka.yml down -v
docker-compose -f docker-compose.kafka.yml up -d
```

### 6.2 포트 충돌

```bash
# 9092 포트 사용 확인 (Windows)
netstat -ano | findstr :9092

# 프로세스 종료 (Windows, 관리자 권한)
taskkill /PID <PID> /F
```

### 6.3 메시지가 소비되지 않는 경우

```bash
# Consumer Group 상태 확인
kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group <group-name>

# Offset 재설정
kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group <group-name> \
  --topic <topic-name> \
  --reset-offsets \
  --to-earliest \
  --execute
```

## 7. 유용한 명령어 모음

```bash
# 모든 토픽 목록
kafka-topics --list --bootstrap-server localhost:9092

# 특정 토픽의 메시지 개수 확인
kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic test-topic

# 토픽의 설정 확인
kafka-configs --describe \
  --bootstrap-server localhost:9092 \
  --entity-type topics \
  --entity-name test-topic

# 파티션 수 증가 (줄이기는 불가능!)
kafka-topics --alter \
  --bootstrap-server localhost:9092 \
  --topic test-topic \
  --partitions 6
```

## 8. 다음 단계

- [ ] Spring Boot 애플리케이션에 Kafka 연동
- [ ] Producer 구현 (주문 완료 이벤트 발행)
- [ ] Consumer 구현 (데이터 플랫폼 전송)
- [ ] 멱등성 설계 적용
- [ ] 에러 처리 및 DLT 구현