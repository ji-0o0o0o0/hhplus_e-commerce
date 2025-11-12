# MySQL 설정 가이드

## 1. 사전 준비
- Docker Desktop 설치 및 실행 필수

## 2. MySQL 컨테이너 실행

```bash
# MySQL 컨테이너 시작
docker-compose up -d

# 컨테이너 상태 확인
docker-compose ps

# MySQL 로그 확인
docker-compose logs -f mysql
```

## 3. MySQL 접속 확인

```bash
# MySQL 컨테이너 접속
docker exec -it hhplus-ecommerce-mysql mysql -u ecommerce -pecommerce1234 ecommerce

# MySQL 내부에서 테이블 확인
SHOW TABLES;

# Flyway 마이그레이션 이력 확인
SELECT * FROM flyway_schema_history;
```

## 4. 컨테이너 관리

```bash
# 컨테이너 중지
docker-compose stop

# 컨테이너 시작
docker-compose start

# 컨테이너 삭제 (데이터는 volume에 유지됨)
docker-compose down

# 컨테이너 및 볼륨 완전 삭제 (데이터 삭제)
docker-compose down -v
```

## 5. 데이터베이스 설정

### 연결 정보
- Host: `localhost`
- Port: `3306`
- Database: `ecommerce`
- Username: `ecommerce`
- Password: `ecommerce1234`

### Spring Boot 설정 (application.properties)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=ecommerce
spring.datasource.password=ecommerce1234
```

## 6. Flyway 마이그레이션

애플리케이션 시작 시 자동으로 실행됩니다:
- `src/main/resources/db/migration/V1__Create_Initial_Schema.sql` 실행
- 테이블 11개 생성 (users, points, products, coupons 등)

### 마이그레이션 파일 규칙
- 파일명 형식: `V{버전}__{설명}.sql`
- 예: `V1__Create_Initial_Schema.sql`, `V2__Add_User_Email.sql`
- 한번 실행된 마이그레이션은 재실행되지 않음

## 7. 문제 해결

### Port 3306 already in use
```bash
# 기존 MySQL 프로세스 확인
netstat -ano | findstr :3306

# 프로세스 종료 또는 docker-compose.yml에서 포트 변경
ports:
  - "3307:3306"  # 호스트 포트를 3307로 변경
```

### Flyway 마이그레이션 실패
```bash
# 컨테이너 및 볼륨 삭제 후 재시작
docker-compose down -v
docker-compose up -d
```

## 8. 테스트 환경

통합 테스트는 Testcontainers를 사용합니다:
- 테스트 실행 시 자동으로 MySQL 컨테이너 생성
- 테스트 완료 후 자동으로 컨테이너 삭제
- `application-test.properties` 설정 사용