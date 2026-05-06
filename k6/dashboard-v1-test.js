import http from 'k6/http';
import { check, sleep } from 'k6';

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

/** =========================================================
 1. 흐름
 superAdmin 로그인
    -> superAdmin 로그인 토큰 입력
 2. 부하 테스트 시나리오
 더미 데이터 유저 10만 건, 소설 5만 건을 넣는다.
 쿼리 분할 + 인덱스 미 적용 시, 가동 속도 및 부하 테스트, N+1 문제를 확인한다.
 ========================================================= */

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

    // V1 API들을 batch로 한 번에 날림
    let responses = http.batch([
        ['GET', `${BASE_URL}/admin/dashboard/v1`, null, params],
        ['GET', `${BASE_URL}/admin/dashboard/v1?novelStatus=`, null, params],
        // 필요하면 통계 v1 API 더 추가하기
    ]);

    console.log("Response Body: " + responses[0].body);
    check(responses[0], { 'v1 status 200': (r) => r.status === 200 });

    sleep(1);
}