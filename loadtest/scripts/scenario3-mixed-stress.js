/**
 * 시나리오 3: 복합 트래픽 (Stress Test)
 *
 * 목적:
 * - 다양한 API 동시 호출 시 영향도 분석
 * - 특정 API의 부하가 다른 API에 미치는 영향 확인
 * - 시스템 전체 한계점 파악
 *
 * 시나리오:
 * - 쿠폰 발급 이벤트 + 일반 쇼핑 트래픽
 * - 워크로드: 쿠폰 30% + 상품 조회 50% + 주문 15% + 포인트 5%
 * - 테스트 시간: 5분
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import {
    BASE_URL,
    headers,
    randomInt,
    randomChoice,
    createStressTestOptions,
} from './utils.js';

// 커스텀 메트릭 (API별 응답 시간)
const couponIssueDuration = new Trend('coupon_issue_duration');
const productViewDuration = new Trend('product_view_duration');
const orderDuration = new Trend('order_duration');
const pointChargeDuration = new Trend('point_charge_duration');

// API별 성공/실패 카운터
const couponIssueSuccess = new Counter('coupon_issue_success');
const productViewSuccess = new Counter('product_view_success');
const orderSuccess = new Counter('order_success');
const pointChargeSuccess = new Counter('point_charge_success');

// 테스트 설정 (최대 500 VU까지 점진적 증가, 2분으로 단축)
export const options = createStressTestOptions(500, 120);

// 테스트 데이터
const COUPON_ID = 1;
const PRODUCT_IDS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
const USER_COUNT = 2000;

// 워크로드 분배 (100% = 쿠폰 30 + 조회 50 + 주문 15 + 포인트 5)
const WORKLOAD = {
    COUPON: 30,
    PRODUCT_VIEW: 80,  // 누적: 30 + 50 = 80
    ORDER: 95,         // 누적: 80 + 15 = 95
    POINT: 100,        // 누적: 95 + 5 = 100
};

export default function () {
    const userId = randomInt(1, USER_COUNT);
    const random = randomInt(1, 100);

    if (random <= WORKLOAD.COUPON) {
        // 30%: 쿠폰 발급
        issueCoupon(userId);
    } else if (random <= WORKLOAD.PRODUCT_VIEW) {
        // 50%: 상품 조회
        viewProduct();
    } else if (random <= WORKLOAD.ORDER) {
        // 15%: 주문 생성
        createOrder(userId);
    } else {
        // 5%: 포인트 충전
        chargePoint(userId);
    }

    sleep(randomInt(1, 3));
}

function issueCoupon(userId) {
    // Kafka 비동기 발급 사용 (Redis 초기화 이슈로 변경)
    const url = `${BASE_URL}/api/coupons/${COUPON_ID}/issue/kafka`;
    const payload = JSON.stringify({ userId });

    const startTime = Date.now();
    const response = http.post(url, payload, { headers });
    const duration = Date.now() - startTime;

    couponIssueDuration.add(duration);

    check(response, {
        '[쿠폰] status is 201 or 400': (r) => r.status === 201 || r.status === 400,
    });

    if (response.status === 201) {
        couponIssueSuccess.add(1);
    }
}

function viewProduct() {
    const productId = randomChoice(PRODUCT_IDS);
    const random = randomInt(1, 100);

    let url;
    if (random <= 70) {
        // 70%: 상품 목록 조회
        const page = randomInt(0, 10);
        url = `${BASE_URL}/api/products?page=${page}&size=20`;
    } else if (random <= 90) {
        // 20%: 상품 상세 조회
        url = `${BASE_URL}/api/products/${productId}`;
    } else {
        // 10%: 인기 상품 조회 (캐시)
        url = `${BASE_URL}/api/products/popular?days=3`;
    }

    const startTime = Date.now();
    const response = http.get(url, { headers });
    const duration = Date.now() - startTime;

    productViewDuration.add(duration);

    check(response, {
        '[상품조회] status is 200': (r) => r.status === 200,
        '[상품조회] response time < 300ms': () => duration < 300,
    });

    if (response.status === 200) {
        productViewSuccess.add(1);
    }
}

function createOrder(userId) {
    // 장바구니 전체 주문 (실제 사용 API)
    const url = `${BASE_URL}/api/orders/cart`;
    const payload = JSON.stringify({
        userId,
        couponId: null
    });

    const startTime = Date.now();
    const response = http.post(url, payload, { headers });
    const duration = Date.now() - startTime;

    orderDuration.add(duration);

    check(response, {
        '[주문] status is 201 or 400': (r) => r.status === 201 || r.status === 400,
    });

    if (response.status === 201) {
        orderSuccess.add(1);
    }
}

function chargePoint(userId) {
    const url = `${BASE_URL}/api/points/charge`;
    const payload = JSON.stringify({
        userId,
        amount: randomInt(10000, 100000)
    });

    const startTime = Date.now();
    const response = http.post(url, payload, { headers });
    const duration = Date.now() - startTime;

    pointChargeDuration.add(duration);

    check(response, {
        '[포인트] status is 200': (r) => r.status === 200,
    });

    if (response.status === 200) {
        pointChargeSuccess.add(1);
    }
}

// 테스트 종료 후 요약
export function handleSummary(data) {
    console.log('\n========== 시나리오 3: 복합 트래픽 결과 ==========');
    console.log(`총 요청 수: ${data.metrics.http_reqs.values.count}`);
    console.log('');
    console.log('[API별 성공 횟수]');
    console.log(`  - 쿠폰 발급: ${data.metrics.coupon_issue_success ? data.metrics.coupon_issue_success.values.count : 0}`);
    console.log(`  - 상품 조회: ${data.metrics.product_view_success ? data.metrics.product_view_success.values.count : 0}`);
    console.log(`  - 주문 생성: ${data.metrics.order_success ? data.metrics.order_success.values.count : 0}`);
    console.log(`  - 포인트 충전: ${data.metrics.point_charge_success ? data.metrics.point_charge_success.values.count : 0}`);
    console.log('');
    console.log('[API별 평균 응답 시간]');
    console.log(`  - 쿠폰 발급: ${data.metrics.coupon_issue_duration ? data.metrics.coupon_issue_duration.values.avg.toFixed(2) : 0}ms`);
    console.log(`  - 상품 조회: ${data.metrics.product_view_duration ? data.metrics.product_view_duration.values.avg.toFixed(2) : 0}ms`);
    console.log(`  - 주문 생성: ${data.metrics.order_duration ? data.metrics.order_duration.values.avg.toFixed(2) : 0}ms`);
    console.log(`  - 포인트 충전: ${data.metrics.point_charge_duration ? data.metrics.point_charge_duration.values.avg.toFixed(2) : 0}ms`);
    console.log('');
    console.log('[전체 응답 시간]');
    console.log(`  - 평균: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms`);
    console.log(`  - P95: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms`);
    console.log(`  - P99: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms`);
    console.log(`  - 최대: ${data.metrics.http_req_duration.values.max.toFixed(2)}ms`);
    console.log('====================================================\n');

    return {
        'stdout': JSON.stringify(data, null, 2),
        '../results/scenario3-mixed-stress.json': JSON.stringify(data, null, 2),
    };
}