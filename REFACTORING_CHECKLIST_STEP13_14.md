# Step13-14 리팩토링 체크리스트

## 📋 진행 상황
- 시작일: 2025-12-11
- 브랜치: step13-14

---

## 🎯 1단계: 코치님 피드백 (최우선 - 성능/안정성)

### [ ] 1. DB 조회 제거 - 유효기간 Redis 관리
**문제**: `requestCouponAsync()`에서 매번 DB 조회 → 트래픽 많을 때 DB 커넥션 병목

**해결**:
- Redis에 쿠폰 유효기간 정보 저장 (`coupon:valid:{couponId}`)
- `RedisCouponRepository`에 메서드 추가:
  - `setCouponValidity(couponId, startDate, endDate, TTL)`
  - `isCouponValid(couponId)`
- `CouponRedisService.requestCouponAsync()`에서 DB 조회 제거

**파일**:
- `src/main/java/com/hhplus/hhplus_ecommerce/coupon/repository/RedisCouponRepository.java`
- `src/main/java/com/hhplus/hhplus_ecommerce/coupon/application/CouponRedisService.java`

---

### [ ] 2. 재고 키 제거 - SCARD로 재고 체크
**문제**: `coupon:stock:{id}` 키를 별도 관리 중 → `coupon:issued:{id}` Set 크기로 확인 가능

**해결**:
- `RedisCouponRepository`에서 삭제:
  - `initializeStock()`
  - `decrementStock()`
  - `incrementStock()`
  - `getStock()`
- `getIssuedCount()` (SCARD) 사용
- `CouponRedisService`에서 재고 체크 로직 변경:
  - `getIssuedCount() >= totalQuantity`

**파일**:
- `src/main/java/com/hhplus/hhplus_ecommerce/coupon/repository/RedisCouponRepository.java`
- `src/main/java/com/hhplus/hhplus_ecommerce/coupon/application/CouponRedisService.java`

---

### [ ] 3. DB 저장 실패 시 재시도 로직 (롤백 → 재시도)
**문제**: DB 저장 실패 시 Redis 롤백 시도 → 롤백도 실패할 수 있음

**해결**:
- `processCouponQueue()` 예외 처리 수정:
  - 현재: `incrementStock()`, `removeIssuedUser()` (롤백)
  - 변경: `addToQueue()` (재시도)
- `CouponIssueRequest`에 `retryCount` 필드 추가
- 최대 재시도 횟수 제한 (예: 3회)

**파일**:
- `src/main/java/com/hhplus/hhplus_ecommerce/coupon/repository/RedisCouponRepository.java`
- `src/main/java/com/hhplus/hhplus_ecommerce/coupon/application/CouponRedisService.java`
- `src/main/java/com/hhplus/hhplus_ecommerce/scheduler/CouponIssueScheduler.java`

---

### [ ] 4. TTL 키 생성 시점에 설정
**문제**: `setExpire()` 호출 시 키가 없으면 실패 → 메모리 누수

**해결**:
- `addIssuedUser()`, `addToQueue()` 메서드에서 키 생성 시 TTL 함께 설정
- 방법 1: 메서드 파라미터에 `Duration ttl` 추가
- 방법 2: 키 생성 후 즉시 `expire()` 호출

**파일**:
- `src/main/java/com/hhplus/hhplus_ecommerce/coupon/repository/RedisCouponRepository.java`

---

### [ ] 5. 관리자 쿠폰 생성 시 Redis 초기화
**문제**: 현재 테스트에서만 `initializeCouponStock()` 호출

**해결**:
- `CouponService.createCoupon()`에서 자동으로 Redis 초기화
- `couponRedisService.initializeCouponStock(couponId)` 호출

**파일**:
- `src/main/java/com/hhplus/hhplus_ecommerce/coupon/application/CouponService.java`

---

### [ ] 6. 랭킹 - 최근 3일간 판매량 집계 로직 복구
**문제**: 전체 상품 랭킹만 있음 → 요구사항: 최근 3일간 판매량

**해결**:
- Sorted Set 키를 날짜별로 분리: `product:ranking:{날짜}`
- 예: `product:ranking:2025-12-11`, `product:ranking:2025-12-10`
- 조회 시 최근 3일 키를 ZUNIONSTORE로 병합
- 스케줄러로 3일 이전 키 자동 삭제

**파일**:
- `src/main/java/com/hhplus/hhplus_ecommerce/product/repository/RedisProductRankingRepository.java`
- `src/main/java/com/hhplus/hhplus_ecommerce/scheduler/StatisticsScheduler.java` (새 파일)

---

### [ ] 7. DB 기반 sorted set 복구 로직
**문제**: Redis 장애 시 랭킹 데이터 복구 방법 없음

**해결**:
- DB에서 주문 데이터 기반으로 Redis sorted set 재생성
- 배치/스케줄러로 주기적 동기화 (선택)
- 관리자 API 추가 (선택): `POST /admin/ranking/rebuild`

**파일**:
- `src/main/java/com/hhplus/hhplus_ecommerce/product/repository/RedisProductRankingRepository.java`
- `src/main/java/com/hhplus/hhplus_ecommerce/scheduler/StatisticsScheduler.java`

---

## 🔧 2단계: Redis 설정 개선

### [ ] 8. GenericJackson2JsonRedisSerializer 변경
**문제**: 패키지 이동 시 캐시 정합성 문제 가능 (defaultTyping 활성화)

**해결**:
- `StringRedisSerializer` 또는 `Jackson2JsonRedisSerializer<T>` 사용
- 참고: https://mangkyu.tistory.com/402

**파일**:
- `src/main/java/com/hhplus/hhplus_ecommerce/config/CacheConfig.java`

---

### [ ] 9. ObjectMapper 빈 주입
**문제**: `CacheConfig`에서 ObjectMapper 새로 생성 (무거운 객체)

**해결**:
- 스프링 빈으로 주입받아 사용
- Redis 전용 설정 필요 시에만 별도 생성

**파일**:
- `src/main/java/com/hhplus/hhplus_ecommerce/config/CacheConfig.java`

---

### [ ] 11. @Value → @ConfigurationProperties
**문제**: `RedisConfig`에서 @Value로 프로퍼티 매핑 (여러 개면 번거로움)

**해결**:
- `@ConfigurationProperties(prefix = "spring.data.redis")` 사용
- 참고: https://mangkyu.tistory.com/207

**파일**:
- `src/main/java/com/hhplus/hhplus_ecommerce/config/RedisConfig.java`

---

## 📝 3단계: 코드 품질 개선

### [ ] 10. 불필요한 주석 제거
**대상**:
- `OrderService.java:42` - `//낙관적락을 통한 주문`
- 기타 코드 그대로 설명하는 주석들

**파일**:
- `src/main/java/com/hhplus/hhplus_ecommerce/order/application/OrderService.java`

---

### [ ] 12. 매직 넘버 상수화
**대상**:
- `CacheConfig`: `Duration.ofMinutes(10)`, `Duration.ofMinutes(30)` 등
- 예: `POPULAR_PRODUCTS_TTL = Duration.ofMinutes(30)`

**파일**:
- `src/main/java/com/hhplus/hhplus_ecommerce/config/CacheConfig.java`

---

### [ ] 13. DTO 네이밍 개선
**문제**: `XXXDto`는 계층 간 이동이 모호

**해결**:
- Controller 입출력: `XXXRequest`, `XXXResponse`
- 내부 전달용: 그대로 유지하거나 다른 네이밍

**대상**:
- `ProductDto` → `ProductResponse`
- `PopularProductDto` → `PopularProductResponse`
- `OrderItemDto` → `OrderItemResponse`

---

## 🏗️ 4단계: 구조 개선

### [ ] 14. Record 클래스 사용
**대상**: DTO 클래스들을 Record로 변환
- `ProductDto`
- `PopularProductDto`
- `OrderItemDto`
- 기타 불변 DTO들

**참고**: https://mangkyu.tistory.com/445

---

### [ ] 15. Service 반환 타입 통일
**현재**:
- 일부: Response DTO 반환 (`getProductsWithPaging`)
- 일부: Entity 반환 (`getProduct`)

**결정 필요**:
- A) Service가 Entity 반환 → Controller에서 DTO 변환 (코치님 선호)
- B) Service가 Response DTO 반환 → Controller 간단

**파일**:
- 모든 Service 클래스들

---

## 📌 메모
- 코치님 피드백: "재고 키 별도 관리 불필요, SCARD 사용"
- 코치님 피드백: "쿠폰 유효성도 Redis로, DB 조회 제거"
- 코치님 피드백: "랭킹도 최근 3일간 집계 요구사항 유지"

---

**작성자**: Claude Code
**마지막 수정**: 2025-12-11