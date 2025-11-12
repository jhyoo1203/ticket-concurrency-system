import http from 'k6/http';
import { Counter } from 'k6/metrics';

// 커스텀 메트릭
const successCounter = new Counter('reservation_success');
const failureCounter = new Counter('reservation_failure');

// 동시성 솔루션 테스트: 1000명이 100개 티켓 예매
export const options = {
  vus: 100,           // 100명의 가상 사용자
  iterations: 1000,   // 총 1000번의 예매 시도
  duration: '30s',    // 최대 30초
};

const BASE_URL = 'http://localhost:8080';
const TICKET_ID = 1;

// 환경 변수로 락 타입 선택 (기본값: optimistic)
// k6 run -e LOCK_TYPE=synchronized level-2-test.js
// k6 run -e LOCK_TYPE=pessimistic level-2-test.js
// k6 run -e LOCK_TYPE=optimistic level-2-test.js
const LOCK_TYPE = __ENV.LOCK_TYPE || 'optimistic';

export function setup() {
  console.log(`🚀 동시성 솔루션 테스트 시작 (락 타입: ${LOCK_TYPE.toUpperCase()})`);
  console.log(`   - 가상 사용자: 100명`);
  console.log(`   - 총 예매 시도: 1000번`);

  const res = http.get(`${BASE_URL}/api/tickets/${TICKET_ID}`);
  if (res.status === 200) {
    const ticket = JSON.parse(res.body);
    console.log(`✅ 초기 재고: ${ticket.stock}개`);
    return { initialStock: ticket.stock, lockType: LOCK_TYPE };
  }

  console.log('❌ 초기 재고 조회 실패');
  return { initialStock: 0, lockType: LOCK_TYPE };
}

export default function (data) {
  const userId = `user_${__VU}_${__ITER}`;

  // 락 타입에 따라 다른 엔드포인트 호출
  let url;
  switch (data.lockType) {
    case 'synchronized':
      url = `${BASE_URL}/api/tickets/${TICKET_ID}/reserve/synchronized?userId=${userId}`;
      break;
    case 'pessimistic':
      url = `${BASE_URL}/api/tickets/${TICKET_ID}/reserve/pessimistic?userId=${userId}`;
      break;
    case 'optimistic':
      url = `${BASE_URL}/api/tickets/${TICKET_ID}/reserve/optimistic?userId=${userId}`;
      break;
    default:
      url = `${BASE_URL}/api/tickets/${TICKET_ID}/reserve/pessimistic?userId=${userId}`;
  }

  const res = http.post(url);

  if (res.status === 200) {
    successCounter.add(1);
  } else {
    failureCounter.add(1);
  }
}

export function teardown(data) {
  console.log('\n📊 테스트 완료');
  console.log('='.repeat(60));

  const res = http.get(`${BASE_URL}/api/tickets/${TICKET_ID}`);
  if (res.status === 200) {
    const ticket = JSON.parse(res.body);
    console.log(`\n📋 최종 결과 (${data.lockType.toUpperCase()}):`);
    console.log(`   초기 재고: ${data.initialStock}개`);
    console.log(`   최종 재고: ${ticket.stock}개`);
    console.log(`   예약 건수: ${ticket.reservationCount}개`);
    console.log(`   차감된 재고: ${data.initialStock - ticket.stock}개`);

    console.log(`\n🔍 동시성 검증:`);
    const hasOverbooking = ticket.reservationCount > data.initialStock;
    const hasRaceCondition = ticket.stock !== data.initialStock - ticket.reservationCount;

    if (hasOverbooking) {
      console.log(`   ❌ 오버부킹 발생! ${ticket.reservationCount - data.initialStock}건 초과 예약됨`);
    } else if (hasRaceCondition) {
      console.log(`   ❌ Race Condition 발생! 재고 차감 오류 발생`);
    } else {
      console.log(`   ✅ 정상: 재고가 정확히 관리되었습니다.`);
    }

    // 각 락 타입별 특징 안내
    console.log(`\n💡 ${data.lockType.toUpperCase()} 방식 특징:`);
    switch (data.lockType) {
      case 'synchronized':
        console.log(`   - 단일 JVM 내에서 동기화 처리`);
        console.log(`   - 단순하고 직관적이지만 분산 환경에서는 동작하지 않음`);
        console.log(`   - 모든 요청이 순차 처리되어 성능 저하 가능`);
        break;
      case 'pessimistic':
        console.log(`   - DB 레벨에서 SELECT ... FOR UPDATE로 락 획득`);
        console.log(`   - 데이터 정합성 보장, 분산 환경에서도 동작`);
        console.log(`   - 락 대기 시간으로 인한 성능 저하 가능`);
        break;
      case 'optimistic':
        console.log(`   - 버전 관리를 통한 낙관적 락`);
        console.log(`   - 락을 사용하지 않아 성능이 좋음`);
        console.log(`   - 충돌 발생 시 재시도 로직 필요`);
        break;
    }
  } else {
    console.log(`\n❌ 최종 결과 조회 실패 (HTTP ${res.status})`);
  }

  console.log('='.repeat(60));
}
