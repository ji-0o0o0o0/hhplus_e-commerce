/**
 * Quick Start 스크립트
 *
 * 목적: k6 설치 확인 및 간단한 API 동작 테스트
 *
 * 실행 방법:
 * k6 run quickstart.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, headers } from './utils.js';

export const options = {
    vus: 10,  // 가상 사용자 10명
    duration: '30s',  // 30초 동안 실행
};

export default function () {
    // 1. 상품 목록 조회
    const productListResponse = http.get(`${BASE_URL}/api/products?page=0&size=10`, { headers });

    check(productListResponse, {
        '[상품 목록] status is 200': (r) => r.status === 200,
        '[상품 목록] response time < 500ms': (r) => r.timings.duration < 500,
    });

    sleep(1);

    // 2. 인기 상품 조회 (캐시)
    const popularResponse = http.get(`${BASE_URL}/api/products/popular?days=3`, { headers });

    check(popularResponse, {
        '[인기 상품] status is 200': (r) => r.status === 200,
        '[인기 상품] response time < 300ms': (r) => r.timings.duration < 300,
    });

    sleep(1);

    // 3. 포인트 잔액 조회
    const userId = __VU;  // VU ID를 사용자 ID로 사용
    const pointResponse = http.get(`${BASE_URL}/api/points?userId=${userId}`, { headers });

    check(pointResponse, {
        '[포인트 조회] status is 200': (r) => r.status === 200,
    });

    sleep(1);
}

export function handleSummary(data) {
    console.log('\n========== Quick Start 테스트 완료 ==========');
    console.log(`총 요청 수: ${data.metrics.http_reqs.values.count}`);
    console.log(`평균 응답 시간: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms`);
    console.log(`P95 응답 시간: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms`);
    console.log(`에러율: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%`);
    console.log('===========================================\n');

    return {
        'stdout': JSON.stringify(data, null, 2),
    };
}