// 공통 유틸리티 함수

export const BASE_URL = 'http://localhost:8080';

// 랜덤 정수 생성
export function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

// 랜덤 배열 요소 선택
export function randomChoice(array) {
    return array[Math.floor(Math.random() * array.length)];
}

// 응답 검증
export function checkResponse(response, expectedStatus = 200) {
    return {
        'status is correct': response.status === expectedStatus,
        'response has body': response.body && response.body.length > 0,
    };
}

// API 공통 헤더
export const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
};

// 테스트 설정 템플릿
export function createLoadTestOptions(vus, duration) {
    return {
        stages: [
            { duration: '30s', target: Math.floor(vus / 2) }, // Warm-up
            { duration: duration, target: vus },               // Full load
            { duration: '30s', target: 0 },                    // Cool-down
        ],
        thresholds: {
            http_req_duration: ['p(95)<500', 'p(99)<1000'],   // 95%는 500ms, 99%는 1s
            http_req_failed: ['rate<0.01'],                   // 에러율 1% 미만
            checks: ['rate>0.95'],                            // 검증 95% 이상 통과
        },
    };
}

export function createPeakTestOptions(vus, duration) {
    return {
        stages: [
            { duration: '10s', target: Math.floor(vus / 4) },  // Quick ramp-up
            { duration: '10s', target: vus },                   // Peak
            { duration: duration, target: vus },                // Sustain
            { duration: '10s', target: 0 },                     // Quick ramp-down
        ],
        thresholds: {
            http_req_duration: ['p(95)<1000', 'p(99)<3000'],
            http_req_failed: ['rate<0.05'],                    // 에러율 5% 미만 (피크 시)
        },
    };
}

export function createStressTestOptions(maxVus, duration) {
    const stages = [];
    const step = Math.floor(maxVus / 10);

    for (let i = 0; i <= 10; i++) {
        stages.push({
            duration: `${Math.floor(duration / 10)}s`,
            target: step * i
        });
    }
    stages.push({ duration: '30s', target: 0 });

    return {
        stages,
        thresholds: {
            http_req_duration: ['p(95)<2000'],
            http_req_failed: ['rate<0.1'],
        },
    };
}

// 커스텀 메트릭
import { Counter, Trend, Rate } from 'k6/metrics';

export const successCounter = new Counter('custom_success_count');
export const failureCounter = new Counter('custom_failure_count');
export const businessLogicDuration = new Trend('custom_business_logic_duration');
export const errorRate = new Rate('custom_error_rate');

// 결과 로깅 헬퍼
export function logResult(scenario, response, startTime) {
    const duration = Date.now() - startTime;

    // 성공: 200, 201, 202 Accepted
    if (response.status === 200 || response.status === 201 || response.status === 202) {
        successCounter.add(1);
        businessLogicDuration.add(duration);
        errorRate.add(0);
    } else {
        failureCounter.add(1);
        errorRate.add(1);
        console.error(`[${scenario}] Error: ${response.status} - ${response.body}`);
    }
}

// 테스트 데이터 생성
export function generateTestUsers(count) {
    const users = [];
    for (let i = 1; i <= count; i++) {
        users.push({
            userId: i,
            email: `user${i}@test.com`,
            name: `TestUser${i}`,
        });
    }
    return users;
}

export function generateTestProducts(count) {
    const products = [];
    for (let i = 1; i <= count; i++) {
        products.push({
            productId: i,
            name: `Product${i}`,
            price: randomInt(1000, 100000),
            stock: randomInt(10, 1000),
        });
    }
    return products;
}

// 결과 요약 출력
export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        'results/summary.json': JSON.stringify(data),
    };
}

import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';