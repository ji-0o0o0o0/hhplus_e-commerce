/**
 * 시나리오 2: 상품 주문 폭주 (Load Test)
 *
 * 목적:
 * - 재고 차감 동시성 검증
 * - 포인트 차감 정합성 확인
 * - DB 트랜잭션 성능 측정
 *
 * 시나리오:
 * - 인기 상품 주문 폭주 (재고 100개)
 * - 목표 TPS: 50-150
 * - 테스트 시간: 2분
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import {
    BASE_URL,
    headers,
    randomInt,
    randomChoice,
    createLoadTestOptions,
    logResult
} from './utils.js';

// 커스텀 메트릭
const orderSuccess = new Counter('order_success');
const orderFailed = new Counter('order_failed');
const stockInsufficientRate = new Rate('stock_insufficient_rate');
const pointInsufficientRate = new Rate('point_insufficient_rate');

// 테스트 설정
export const options = createLoadTestOptions(100, '2m');

// 테스트 데이터
const PRODUCT_IDS = [1, 2, 3, 4, 5];  // 인기 상품 5개
const USER_COUNT = 1000;
const CHARGE_AMOUNT = 100000;  // 포인트 충전 금액 (10만원)

// Setup: 테스트 시작 전 사용자에게 포인트 충전
export function setup() {
    console.log('========== Setup: 테스트 사용자 포인트 충전 ==========');

    const userIds = [];
    for (let i = 1; i <= 50; i++) {  // 50명의 사용자에게 포인트 충전
        chargePoint(i, CHARGE_AMOUNT);
        userIds.push(i);
        if (i % 10 === 0) {
            console.log(`포인트 충전 진행 중: ${i}/50`);
        }
    }

    console.log('포인트 충전 완료');
    return { userIds };
}

function chargePoint(userId, amount) {
    const url = `${BASE_URL}/api/points/charge`;
    const payload = JSON.stringify({ userId, amount });
    const response = http.post(url, payload, { headers });

    if (response.status !== 200) {
        console.error(`포인트 충전 실패: UserId ${userId}, Status: ${response.status}`);
    }
}

// 테스트 시나리오
export default function (data) {
    const userId = randomChoice(data.userIds);
    const productId = randomChoice(PRODUCT_IDS);

    // 1. 장바구니에 상품 추가
    const cartItemId = addToCart(userId, productId, 1);

    if (!cartItemId) {
        sleep(1);
        return;
    }

    sleep(0.3);

    // 2. 주문 생성 (포인트 결제)
    createOrder(userId);

    sleep(0.5);
}

function addToCart(userId, productId, quantity) {
    const url = `${BASE_URL}/api/carts/items`;
    const payload = JSON.stringify({ userId, productId, quantity });

    const response = http.post(url, payload, { headers });

    check(response, {
        '[장바구니] status is 201': (r) => r.status === 201,
    });

    if (response.status === 201) {
        const body = JSON.parse(response.body);
        return body.data.cartItemId;
    }

    return null;
}

function createOrder(userId, cartItemIds) {
    // 장바구니 전체 주문 사용 (주 API)
    const url = `${BASE_URL}/api/orders/cart`;
    const payload = JSON.stringify({
        userId,
        couponId: null
    });

    const startTime = Date.now();
    const response = http.post(url, payload, { headers });
    const duration = Date.now() - startTime;

    const checkResult = check(response, {
        '[주문] status is 201 or 400': (r) => r.status === 201 || r.status === 400,
        '[주문] response time < 2000ms': () => duration < 2000,
    });

    if (response.status === 201) {
        orderSuccess.add(1);
        const body = JSON.parse(response.body);
        console.log(`[장바구니 주문 성공] UserId: ${userId}, OrderId: ${body.data.id}, Duration: ${duration}ms`);
    } else if (response.status === 400) {
        const body = JSON.parse(response.body);

        if (body.message && body.message.includes('재고')) {
            stockInsufficientRate.add(1);
            console.log(`[주문 실패] 재고 부족 - UserId: ${userId}`);
        } else if (body.message && body.message.includes('포인트')) {
            pointInsufficientRate.add(1);
            console.log(`[주문 실패] 포인트 부족 - UserId: ${userId}`);
        } else if (body.message && body.message.includes('장바구니')) {
            console.log(`[주문 실패] 장바구니 비어있음 - UserId: ${userId}`);
        } else {
            orderFailed.add(1);
            console.error(`[주문 실패] UserId: ${userId}, Message: ${body.message}`);
        }
    } else {
        orderFailed.add(1);
        console.error(`[주문 실패] UserId: ${userId}, Status: ${response.status}, Body: ${response.body}`);
    }

    logResult('Create Order from Cart', response, startTime);
}

// 테스트 종료 후 검증
export function teardown(data) {
    console.log('\n========== Teardown: 재고 및 포인트 정합성 검증 ==========');

    // 상품 재고 확인
    PRODUCT_IDS.forEach(productId => {
        const url = `${BASE_URL}/api/products/${productId}`;
        const response = http.get(url, { headers });

        if (response.status === 200) {
            const body = JSON.parse(response.body);
            console.log(`Product ${productId}: Stock = ${body.data.stock}`);
        }
    });

    // 포인트 잔액 확인 (샘플)
    data.userIds.slice(0, 5).forEach(userId => {
        const url = `${BASE_URL}/api/points?userId=${userId}`;
        const response = http.get(url, { headers });

        if (response.status === 200) {
            const body = JSON.parse(response.body);
            console.log(`User ${userId}: Point = ${body.data.amount}`);
        }
    });
}

// 테스트 종료 후 요약
export function handleSummary(data) {
    console.log('\n========== 시나리오 2: 상품 주문 폭주 결과 ==========');
    console.log(`총 요청 수: ${data.metrics.http_reqs.values.count}`);
    console.log(`성공 주문: ${data.metrics.order_success ? data.metrics.order_success.values.count : 0}`);
    console.log(`실패 주문: ${data.metrics.order_failed ? data.metrics.order_failed.values.count : 0}`);
    console.log(`재고 부족율: ${(data.metrics.stock_insufficient_rate ? data.metrics.stock_insufficient_rate.values.rate * 100 : 0).toFixed(2)}%`);
    console.log(`포인트 부족율: ${(data.metrics.point_insufficient_rate ? data.metrics.point_insufficient_rate.values.rate * 100 : 0).toFixed(2)}%`);
    console.log(`평균 응답 시간: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms`);
    console.log(`P95 응답 시간: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms`);
    console.log('====================================================\n');

    return {
        'stdout': JSON.stringify(data, null, 2),
        '../results/scenario2-order-load.json': JSON.stringify(data, null, 2),
    };
}