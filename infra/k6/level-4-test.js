import http from 'k6/http';
import { Counter } from 'k6/metrics';
import { check, sleep } from 'k6';

// 커스텀 메트릭
const successCounter = new Counter('reservation_success');
const failureCounter = new Counter('reservation_failure');

// LV.4: Kafka 메시지 큐 부하 테스트 (다중 인스턴스 + 비동기 처리)
// 점진적으로 부하를 증가시켜 서버 한계 테스트 (3개 인스턴스 + Nginx + Kafka)
export const options = {
  stages: [
    { duration: '10s', target: 500 },   // 10초 동안 500명까지 증가
    { duration: '20s', target: 2000 },  // 20초 동안 2000명까지 증가
    { duration: '20s', target: 2000 },  // 20초 동안 2000명 유지 (최대 부하)
    { duration: '10s', target: 0 },     // 10초 동안 0명으로 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<5000'], // 95%의 요청이 5초 이내 응답
    http_req_failed: ['rate<0.1'],     // 실패율 10% 미만
  },
};

// Nginx 로드밸런서를 통해 3개의 애플리케이션 인스턴스로 요청 분산
const BASE_URL = 'http://localhost:8080';
const TICKET_ID = 1;

export function setup() {
  console.log('🚀 LV.4 Kafka 메시지 큐 부하 테스트 시작');
  console.log('📋 테스트 시나리오: 점진적 부하 증가 (0→500→2000명, 60초간)');
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

  // Kafka 비동기 예매 엔드포인트 호출
  const url = `${BASE_URL}/api/tickets/${TICKET_ID}/reserve?userId=${userId}`;
  const res = http.post(url);

  // 응답 체크 (비동기이므로 200 OK만 확인)
  const success = check(res, {
    'status is 200': (r) => r.status === 200,
  });

  if (res.status === 200) {
    successCounter.add(1);
  } else {
    failureCounter.add(1);
  }
}

export function teardown(data) {
  console.log('\n📊 테스트 완료 - Kafka 메시지 처리 대기 중...');
  console.log('='.repeat(60));

  // Kafka Consumer가 메시지를 처리할 시간 확보 (대량 요청이므로 충분한 시간 필요)
  console.log('⏳ 20초 대기 중 (Kafka Consumer 처리 시간)...');
  sleep(20);

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

    console.log(`\n🔍 동시성 검증 (Kafka + Redisson):`);

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
  } else {
    console.log('❌ 최종 티켓 정보를 가져올 수 없습니다.');
  }

  console.log('='.repeat(60));
}
