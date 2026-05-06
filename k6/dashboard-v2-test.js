import http from 'k6/http';
import { check, sleep } from 'k6';

/** =========================================================
 1. 흐름
 superAdmin 로그인
    -> superAdmin 로그인 토큰 입력
 2. 부하 테스트 시나리오
 더미 데이터 유저 10만 건, 소설 5만 건을 넣는다.
 쿼리 통합 + 인덱스 적용 시, 가동 속도 및 부하 테스트, N+1 문제를 확인한다.
 ========================================================= */

export const options = {
    stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000'],
    },
};

const BASE_URL = 'http://localhost:8080/api';

// 포스트맨 발급 토큰 넣기
const TOKEN = 'POSTMAN_ACCESS_TOKEN'

export default function () {
    const params = {
        headers: {
            'Authorization': `Bearer ${TOKEN}`,
            'Content-Type': 'application/json',
        },
    };

    // V2 병합 API 호출
    let res1 = http.get(`${BASE_URL}/admin/dashboard/v2`, params);
    let res2 = http.get(`${BASE_URL}/admin/dashboard/v2?novelStatus=`, params);

    check(res1, { 'v2 status 200': (r) => r.status === 200 });

    sleep(1);
}