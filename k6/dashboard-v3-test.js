import http from 'k6/http';
import { check, sleep } from 'k6';

/** =========================================================
 1. 흐름
 superAdmin 로그인
    -> superAdmin 로그인 토큰 입력
 2. 부하 테스트 시나리오
 더미 데이터 유저 10만 건, 소설 5만 건을 넣는다.
 쿼리 병합 + 인덱스 적용 + Redis 캐시(신규 회원/신작 소설) 적용 시, 가동 속도 및 부하 테스트, N+1 문제를 확인한다.
 ========================================================= */

// V1, V2와 완전히 동일한 부하 조건
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
            'Authorization': `Bearer ${TOKEN}`, // 다시 Bearer 방식으로 원복
            'Content-Type': 'application/json',
        },
        // 이 태그가 있어야 그라파나에서 V3 그래프가 보임
        tags: {
            name: 'V3_Redis_Live'
        }
    };

    // V3 전용 API 호출
    let res1 = http.get(`${BASE_URL}/admin/dashboard/live`, params);

    // 응답 200 확인
    check(res1, { 'v3 status 200': (r) => r.status === 200 });

    // V1, V2와 동일하게 1초 대기
    sleep(1);
}