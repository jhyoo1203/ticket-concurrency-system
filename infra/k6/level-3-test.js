import http from 'k6/http';
import { Counter } from 'k6/metrics';
import { check, sleep } from 'k6';

// 커스텀 메트릭
const successCounter = new Counter('reservation_success');
const failureCounter = new Counter('reservation_failure');
const lockTimeoutCounter = new Counter('lock_timeout');

// LV.3: Redisson 분산 락 동시성 테스트
// 1000명이 동시에 100개 티켓 예매 시도
export const options = {
  vus: 100,           // 100명의 가상 사용자
  iterations: 1000,   // 총 1000번의 예매 시도
  duration: '30s',    // 최대 30초
};

const BASE_URL = 'http://localhost:8080';
const TICKET_ID = 1;

export function setup() {
  console.log('🚀 LV.3 Redisson 분산 락 테스트 시작');
  console.log('📋 테스트 시나리오: 1000명 → 100개 티켓 (분산 락으로 중복 구매 방지)');
  console.log('='.repeat(60));

  const res = http.get(`${BASE_URL}/api/tickets/${TICKET_ID}`);
  if (res.status === 200) {
    const ticket = JSON.parse(res.body);
    console.log(`✅ 초기 재고: ${ticket.stock}개`);
    console.log(`✅ 기존 예약 건수: ${ticket.reservationCount}개`);
    return {
      initialStock: ticket.stock,
      initialReservationCount: ticket.reservationCount
    };
  }

  console.log('❌ 초기 티켓 정보를 가져올 수 없습니다.');
  return { initialStock: 0, initialReservationCount: 0 };
}

export default function (data) {
  const userId = `user_${__VU}_${__ITER}`;

  // Redisson Lock 엔드포인트 호출
  const url = `${BASE_URL}/api/tickets/${TICKET_ID}/reserve?userId=${userId}`;
  const res = http.post(url);

  // 응답 체크
  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'status is 400': (r) => r.status === 400, // 재고 부족, 중복 예매 등
  });

  if (res.status === 200) {
    successCounter.add(1);
  } else if (res.status === 400) {
    failureCounter.add(1);

    // 락 타임아웃 여부 확인
    if (res.body && res.body.includes('예매 처리 중입니다')) {
      lockTimeoutCounter.add(1);
    }
  } else {
    failureCounter.add(1);
  }
}

export function teardown(data) {
  console.log('\n📊 테스트 완료');
  console.log('='.repeat(60));

  // 잠시 대기 (마지막 트랜잭션이 커밋될 시간 확보)
  sleep(2);

  const res = http.get(`${BASE_URL}/api/tickets/${TICKET_ID}`);
  if (res.status === 200) {
    const ticket = JSON.parse(res.body);

    console.log(`\n📋 최종 결과:`);
    console.log(`   초기 재고: ${data.initialStock}개`);
    console.log(`   최종 재고: ${ticket.stock}개`);
    console.log(`   초기 예약 건수: ${data.initialReservationCount}개`);
    console.log(`   최종 예약 건수: ${ticket.reservationCount}개`);
    console.log(`   새로운 예약: ${ticket.reservationCount - data.initialReservationCount}개`);
    console.log(`   차감된 재고: ${data.initialStock - ticket.stock}개`);

    console.log(`\n🔍 동시성 검증 (Redisson 분산 락):`);

    const newReservations = ticket.reservationCount - data.initialReservationCount;
    const stockDecreased = data.initialStock - ticket.stock;

    if (newReservations > data.initialStock) {
      console.log(`   ❌ 오버부킹 발생! ${newReservations - data.initialStock}건 초과 예약됨`);
    } else if (stockDecreased !== newReservations) {
      console.log(`   ❌ Race Condition 발생! 재고 차감(${stockDecreased}) != 예약 건수(${newReservations})`);
    } else if (ticket.stock < 0) {
      console.log(`   ❌ 음수 재고 발생! 최종 재고: ${ticket.stock}개`);
    } else {
      console.log(`   ✅ 정상: 재고가 정확히 관리되었습니다.`);
      console.log(`   ✅ 정합성 검증: 재고 차감(${stockDecreased}) = 예약 건수(${newReservations})`);
    }

    console.log(`\n💡 Trade-off 확인:`);
    console.log(`   - 분산 락도 '락'이므로 대기 시간이 발생합니다.`);
    console.log(`   - 하지만 복잡한 비즈니스 로직(중복 구매 방지 + 재고 차감)의 정합성을 보장합니다.`);
    console.log(`   - Redis 장애 시 시스템 전체에 영향을 미칠 수 있습니다.`);

    console.log(`\n🎯 LV.3의 장점:`);
    console.log(`   ✅ 분산 환경(멀티 인스턴스)에서도 동작`);
    console.log(`   ✅ 중복 구매 방지 + 재고 차감을 원자적으로 처리`);
    console.log(`   ✅ DB 부하가 Pessimistic Lock보다 적음`);
    console.log(`   ✅ Pub/Sub 방식으로 효율적인 대기 (Spin Lock 아님)`);

    console.log(`\n⚠️  LV.3의 한계:`);
    console.log(`   ⚠️  락 대기 시간(Latency)은 여전히 존재`);
    console.log(`   ⚠️  Redis 장애 시 시스템 전체 영향 (SPoF)`);
    console.log(`   ⚠️  네트워크 오버헤드 발생`);
  } else {
    console.log('❌ 최종 티켓 정보를 가져올 수 없습니다.');
  }

  console.log('='.repeat(60));
}
