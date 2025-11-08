import http from 'k6/http';
import { Counter } from 'k6/metrics';

// 커스텀 메트릭
const successCounter = new Counter('reservation_success');
const failureCounter = new Counter('reservation_failure');

// 간단한 동시성 테스트: 1000명이 100개 티켓 예매
export const options = {
  vus: 100,           // 100명의 가상 사용자
  iterations: 1000,   // 총 1000번의 예매 시도
  duration: '30s',    // 최대 30초
};

const BASE_URL = 'http://localhost:8080';
const TICKET_ID = 1;

export function setup() {
  console.log('🚀 동시성 테스트 시작 (1000명 → 100개 티켓)');

  const res = http.get(`${BASE_URL}/api/tickets/${TICKET_ID}`);
  if (res.status === 200) {
    const ticket = JSON.parse(res.body);
    console.log(`✅ 초기 재고: ${ticket.stock}개`);
    return { initialStock: ticket.stock };
  }

  return { initialStock: 0 };
}

export default function (data) {
  const userId = `user_${__VU}_${__ITER}`;

  const url = `${BASE_URL}/api/tickets/${TICKET_ID}/reserve?userId=${userId}`;
  const res = http.post(url);

  if (res.status === 200) {
    successCounter.add(1);
  } else {
    failureCounter.add(1);
  }
}

export function teardown(data) {
  console.log('\n📊 테스트 완료');
  console.log('='.repeat(50));

  const res = http.get(`${BASE_URL}/api/tickets/${TICKET_ID}`);
  if (res.status === 200) {
    const ticket = JSON.parse(res.body);
    console.log(`\n📋 최종 결과:`);
    console.log(`   초기 재고: ${data.initialStock}개`);
    console.log(`   최종 재고: ${ticket.stock}개`);
    console.log(`   예약 건수: ${ticket.reservationCount}개`);
    console.log(`   차감된 재고: ${data.initialStock - ticket.stock}개`);

    console.log(`\n🔍 동시성 검증:`);
    if (ticket.reservationCount > data.initialStock) {
      console.log(`   ❌ 오버부킹 발생! ${ticket.reservationCount - data.initialStock}건 초과 예약됨`);
    } else if (ticket.stock !== data.initialStock - ticket.reservationCount) {
      console.log(`   ❌ Race Condition 발생! 재고 차감 오류 발생`);
    } else {
      console.log(`   ✅ 정상: 재고가 정확히 관리되었습니다.`);
    }
  }

  console.log('='.repeat(50));
}
