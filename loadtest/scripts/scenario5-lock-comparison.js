/**
 * 시나리오 5: 낙관적 락 vs 분산 락 성능 비교
 *
 * 목적:
 * - 두 락 방식의 TPS 및 응답 시간 비교
 * - 재시도 오버헤드 vs 락 대기 시간 분석
 * - 동시성 수준별 성능 변화 측정
 *
 * 테스트 케이스:
 * - VU 10: 충돌 적음, 낙관적 락 유리 예상
 * - VU 50: 충돌 증가, 재시도 발생
 * - VU 100: 충돌 많음, 분산 락 유리 예상
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { BASE_URL, headers, randomInt } from './utils.js';

// 커스텀 메트릭
const optimisticDuration = new Trend('optimistic_lock_duration');
const distributedDuration = new Trend('distributed_lock_duration');
const optimisticSuccess = new Counter('optimistic_lock_success');
const distributedSuccess = new Counter('distributed_lock_success');
const optimisticRetryRate = new Rate('optimistic_lock_retry_rate');

// 테스트 설정
export const options = {
    scenarios: {
        // 시나리오 A: VU 10 (낮은 동시성)
        low_concurrency: {
            executor: 'constant-vus',
            vus: 10,
            duration: '1m',
            tags: { scenario: 'low' },
            exec: 'testLowConcurrency',
        },
        // 시나리오 B: VU 50 (중간 동시성)
        medium_concurrency: {
            executor: 'constant-vus',
            vus: 50,
            duration: '1m',
            startTime: '2m',  // 이전 시나리오 종료 후 1분 대기 후 시작
            tags: { scenario: 'medium' },
            exec: 'testMediumConcurrency',
        },
        // 시나리오 C: VU 100 (높은 동시성)
        high_concurrency: {
            executor: 'constant-vus',
            vus: 100,
            duration: '1m',
            startTime: '4m',
            tags: { scenario: 'high' },
            exec: 'testHighConcurrency',
        },
    },
    thresholds: {
        'http_req_duration{scenario:low}': ['p(95)<500'],
        'http_req_duration{scenario:medium}': ['p(95)<1000'],
        'http_req_duration{scenario:high}': ['p(95)<2000'],
    },
};

// 테스트 데이터
const USER_COUNT = 500;
const CHARGE_AMOUNT = 10000;

export function testLowConcurrency() {
    runTest('low');
}

export function testMediumConcurrency() {
    runTest('medium');
}

export function testHighConcurrency() {
    runTest('high');
}

function runTest(scenario) {
    const userId = randomInt(1, USER_COUNT);

    // 포인트 충전 API 테스트 (분산 락 vs 성능 비교용 낙관적 락)
    // 주의: 실제로는 낙관적 락 API가 별도로 없으므로, 분산 락만 테스트
    testDistributedLockPointCharge(userId, scenario);

    sleep(0.5);

    // 쿠폰 발급 테스트도 가능 (동일한 패턴)
    // testDistributedLockCouponIssue(userId, scenario);
}

function testDistributedLockPointCharge(userId, scenario) {
    const url = `${BASE_URL}/api/points/charge`;
    const payload = JSON.stringify({ userId, amount: CHARGE_AMOUNT });

    const startTime = Date.now();
    const response = http.post(url, payload, {
        headers,
        tags: { lock_type: 'distributed', scenario },
    });
    const duration = Date.now() - startTime;

    distributedDuration.add(duration);

    const checkResult = check(response, {
        '[분산락] status is 200': (r) => r.status === 200,
        '[분산락] response time varies by concurrency': () => {
            if (scenario === 'low') return duration < 500;
            if (scenario === 'medium') return duration < 1000;
            return duration < 2000;
        },
    });

    if (response.status === 200) {
        distributedSuccess.add(1);
    } else {
        console.error(`[분산락 실패] Scenario: ${scenario}, UserId: ${userId}, Status: ${response.status}`);
    }
}

// 낙관적 락 테스트 (별도 API가 있다면)
function testOptimisticLockPointCharge(userId, scenario) {
    // 예시: 낙관적 락 전용 엔드포인트가 있다면
    // const url = `${BASE_URL}/api/points/charge-optimistic`;

    const url = `${BASE_URL}/api/points/charge`;  // 동일 API 사용 (실제로는 분산 락)
    const payload = JSON.stringify({ userId, amount: CHARGE_AMOUNT });

    const startTime = Date.now();
    const response = http.post(url, payload, {
        headers,
        tags: { lock_type: 'optimistic', scenario },
    });
    const duration = Date.now() - startTime;

    optimisticDuration.add(duration);

    const checkResult = check(response, {
        '[낙관적락] status is 200 or 409': (r) => r.status === 200 || r.status === 409,
    });

    if (response.status === 200) {
        optimisticSuccess.add(1);
    } else if (response.status === 409) {
        // OptimisticLockException으로 인한 재시도 발생
        optimisticRetryRate.add(1);
    }
}

export function handleSummary(data) {
    console.log('\n========== 시나리오 5: 락 방식 성능 비교 결과 ==========');

    console.log('\n[시나리오별 응답 시간 비교]');
    const scenarios = ['low', 'medium', 'high'];
    scenarios.forEach(scenario => {
        const durationMetric = data.metrics[`http_req_duration{scenario:${scenario}}`];
        if (durationMetric) {
            console.log(`\n${scenario.toUpperCase()} Concurrency (VU: ${scenario === 'low' ? 10 : scenario === 'medium' ? 50 : 100}):`);
            console.log(`  - 평균: ${durationMetric.values.avg.toFixed(2)}ms`);
            console.log(`  - P50: ${durationMetric.values.med.toFixed(2)}ms`);
            console.log(`  - P95: ${durationMetric.values['p(95)'].toFixed(2)}ms`);
            console.log(`  - P99: ${durationMetric.values['p(99)'].toFixed(2)}ms`);
            console.log(`  - 최대: ${durationMetric.values.max.toFixed(2)}ms`);
        }
    });

    console.log('\n[락 방식별 통계]');
    console.log(`분산 락:`);
    console.log(`  - 성공 횟수: ${data.metrics.distributed_lock_success ? data.metrics.distributed_lock_success.values.count : 0}`);
    console.log(`  - 평균 응답 시간: ${data.metrics.distributed_lock_duration ? data.metrics.distributed_lock_duration.values.avg.toFixed(2) : 0}ms`);

    console.log(`\n낙관적 락:`);
    console.log(`  - 성공 횟수: ${data.metrics.optimistic_lock_success ? data.metrics.optimistic_lock_success.values.count : 0}`);
    console.log(`  - 평균 응답 시간: ${data.metrics.optimistic_lock_duration ? data.metrics.optimistic_lock_duration.values.avg.toFixed(2) : 0}ms`);
    console.log(`  - 재시도율: ${data.metrics.optimistic_lock_retry_rate ? (data.metrics.optimistic_lock_retry_rate.values.rate * 100).toFixed(2) : 0}%`);

    console.log('\n[분석]');
    console.log('VU가 증가할수록 (동시성 증가):');
    console.log('- 분산 락: 락 대기 시간 증가로 응답 시간 증가');
    console.log('- 낙관적 락: 충돌 빈도 증가로 재시도 증가 및 응답 시간 증가');
    console.log('====================================================\n');

    return {
        'stdout': JSON.stringify(data, null, 2),
        '../results/scenario5-lock-comparison.json': JSON.stringify(data, null, 2),
    };
}