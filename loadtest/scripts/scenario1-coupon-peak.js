/**
 * 시나리오 1: 선착순 쿠폰 발급 (Peak Test)
 *
 * 목적:
 * - 동시 접속 폭주 상황에서 시스템 안정성 검증
 * - 분산 락 vs Kafka 비동기 처리 성능 비교
 * - Consumer Lag 모니터링
 *
 * 시나리오:
 * - 선착순 1000명 한정 쿠폰 이벤트
 * - 목표 TPS: 100-300
 * - 테스트 시간: 1분
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import {
    BASE_URL,
    headers,
    randomInt,
    createPeakTestOptions,
    logResult
} from './utils.js';

// 커스텀 메트릭
const couponIssueSuccess = new Counter('coupon_issue_success');
const couponIssueFailed = new Counter('coupon_issue_failed');
const duplicateIssueRate = new Rate('duplicate_issue_rate');
const soldOutRate = new Rate('sold_out_rate');

// 테스트 설정
export const options = createPeakTestOptions(300, '1m');

// 테스트 데이터 (실제로는 DB에서 조회하거나 사전 생성)
const USER_COUNT = 5000;  // 5000명의 사용자가 1000개 쿠폰 경쟁

// 테스트 시나리오
export default function () {
    const userId = randomInt(1, USER_COUNT);
    const random = randomInt(1, 100);

    if (random <= 50) {
        // 50%: 분산 락 동기 발급
        testSyncCouponIssue(userId);
    } else {
        // 50%: Kafka 비동기 발급
        testKafkaCouponIssue(userId);
    }

    sleep(0.5);
}

function testSyncCouponIssue(userId) {
    const COUPON_ID = 1;  // 테스트용 쿠폰 ID
    const url = `${BASE_URL}/api/coupons/issue`;
    const payload = JSON.stringify({ userId, couponId: COUPON_ID });

    const startTime = Date.now();
    const response = http.post(url, payload, { headers });
    const duration = Date.now() - startTime;

    const checkResult = check(response, {
        '[동기] status is 201 or 409': (r) => r.status === 201 || r.status === 409,
        '[동기] response time < 1000ms': () => duration < 1000,
    });

    if (response.status === 201) {
        couponIssueSuccess.add(1);
        console.log(`[동기 발급 성공] UserId: ${userId}, Duration: ${duration}ms`);
    } else if (response.status === 409) {
        const body = JSON.parse(response.body);
        if (body.message && body.message.includes('이미 발급')) {
            duplicateIssueRate.add(1);
        } else if (body.message && body.message.includes('품절')) {
            soldOutRate.add(1);
            console.log('[동기 발급] 쿠폰 품절');
        }
    } else {
        couponIssueFailed.add(1);
        console.error(`[동기 발급 실패] UserId: ${userId}, Status: ${response.status}, Body: ${response.body}`);
    }

    logResult('Sync Coupon Issue', response, startTime);
}

// Kafka 비동기 발급 테스트
function testKafkaCouponIssue(userId) {
    const COUPON_ID = 2;
    const url = `${BASE_URL}/api/coupons/${COUPON_ID}/issue-kafka`;
    const payload = JSON.stringify({ userId, couponId: COUPON_ID });

    const startTime = Date.now();
    const response = http.post(url, payload, { headers });
    const duration = Date.now() - startTime;

    const checkResult = check(response, {
        '[Kafka 비동기] status is 202 or 409': (r) => r.status === 202 || r.status === 409,
        '[Kafka 비동기] response time < 500ms': () => duration < 500,
    });

    if (response.status === 202) {
        const body = JSON.parse(response.body);
        const requestId = body.data.requestId;

        couponIssueSuccess.add(1);
        console.log(`[Kafka 비동기 발급 성공] UserId: ${userId}, RequestId: ${requestId}, Duration: ${duration}ms`);

        // 발급 상태 확인 (선택적)
        // sleep(2);
        // checkCouponIssueStatus(requestId);

    } else if (response.status === 409) {
        const body = JSON.parse(response.body);
        if (body.message && body.message.includes('이미 발급')) {
            duplicateIssueRate.add(1);
        } else if (body.message && body.message.includes('품절')) {
            soldOutRate.add(1);
            console.log('[Kafka 비동기 발급] 쿠폰 품절');
        }
    } else {
        couponIssueFailed.add(1);
        console.error(`[Kafka 비동기 발급 실패] UserId: ${userId}, Status: ${response.status}, Body: ${response.body}`);
    }

    logResult('Kafka Async Coupon Issue', response, startTime);
}

function checkCouponIssueStatus(requestId) {
    const url = `${BASE_URL}/api/coupons/issue-status/${requestId}`;
    const response = http.get(url, { headers });

    check(response, {
        '[상태 조회] status is 200': (r) => r.status === 200,
        '[상태 조회] has status field': (r) => {
            const body = JSON.parse(r.body);
            return body.data && body.data.status !== undefined;
        },
    });

    if (response.status === 200) {
        const body = JSON.parse(response.body);
        console.log(`[발급 상태] RequestId: ${requestId}, Status: ${body.data.status}`);
    }
}

// 테스트 종료 후 요약
export function handleSummary(data) {
    console.log('\n========== 시나리오 1: 선착순 쿠폰 발급 결과 ==========');
    console.log(`총 요청 수: ${data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 0}`);
    console.log(`성공 발급: ${data.metrics.coupon_issue_success ? data.metrics.coupon_issue_success.values.count : 0}`);
    console.log(`실패 발급: ${data.metrics.coupon_issue_failed ? data.metrics.coupon_issue_failed.values.count : 0}`);

    const duplicateRate = data.metrics.duplicate_issue_rate ? data.metrics.duplicate_issue_rate.values.rate : 0;
    const soldOutRate = data.metrics.sold_out_rate ? data.metrics.sold_out_rate.values.rate : 0;
    console.log(`중복 발급율: ${(duplicateRate * 100).toFixed(2)}%`);
    console.log(`품절율: ${(soldOutRate * 100).toFixed(2)}%`);

    if (data.metrics.http_req_duration && data.metrics.http_req_duration.values) {
        const values = data.metrics.http_req_duration.values;
        if (values.avg) console.log(`평균 응답 시간: ${values.avg.toFixed(2)}ms`);
        if (values['p(95)']) console.log(`P95 응답 시간: ${values['p(95)'].toFixed(2)}ms`);
        if (values['p(99)']) console.log(`P99 응답 시간: ${values['p(99)'].toFixed(2)}ms`);
    }
    console.log('====================================================\n');

    return {
        'stdout': JSON.stringify(data, null, 2),
        '../results/scenario1-coupon-peak.json': JSON.stringify(data, null, 2),
    };
}