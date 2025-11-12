package com.ticket.lv3redisson;

import com.ticket.lv3redisson.domain.Ticket;
import com.ticket.lv3redisson.infrastructure.ReservationRepository;
import com.ticket.lv3redisson.infrastructure.TicketRepository;
import com.ticket.lv3redisson.application.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LV.3: Redisson 분산 락을 이용한 동시성 문제 해결 테스트
 *
 * Redis 기반 분산 락으로 복잡한 비즈니스 로직(중복 구매 확인 + 재고 차감)을 원자적으로 처리
 */
@SpringBootTest
class ConcurrencySolutionTest {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencySolutionTest.class);

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private static final int INITIAL_STOCK = 100;
    private static final int CONCURRENT_USERS = 1000;

    @BeforeEach
    void setUp() {
        // 데이터 초기화
        reservationRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    @Test
    @DisplayName("[LV.3 성공] Redisson 분산 락 - 1000명이 동시에 100개 티켓 예매")
    void testRedissonDistributedLock() throws InterruptedException {
        // given
        Ticket ticket = new Ticket("Redisson 분산 락 테스트 티켓", INITIAL_STOCK);
        ticket = ticketRepository.save(ticket);
        Long ticketId = ticket.getId();

        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        long startTime = System.currentTimeMillis();

        try (ExecutorService executorService = Executors.newFixedThreadPool(100)) {
            for (int i = 0; i < CONCURRENT_USERS; i++) {
                final String userId = "user-" + i;
                executorService.submit(() -> {
                    try {
                        ticketService.reserveTicketWithRedissonLock(ticketId, userId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        if (failCount.get() <= 5) { // 처음 5개만 로깅
                            log.warn("[Redisson Lock Test] 예외 발생: {}", e.getMessage());
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // then
        Ticket result = ticketService.getTicket(ticketId);
        long reservationCount = ticketService.getReservationCount(ticketId);

        printTestResult("Redisson 분산 락", duration, successCount.get(), failCount.get(), result.getStock(), reservationCount);

        assertThat(result.getStock()).isEqualTo(0);
        assertThat(reservationCount).isEqualTo(INITIAL_STOCK);
    }

    @Test
    @DisplayName("[LV.3 Trade-off 확인] 락 대기 시간이 존재함")
    void testRedissonLockLatency() throws InterruptedException {
        // given
        Ticket ticket = new Ticket("Latency 테스트 티켓", 10);
        ticket = ticketRepository.save(ticket);
        Long ticketId = ticket.getId();

        int userCount = 50; // 10개 재고에 50명 요청
        CountDownLatch latch = new CountDownLatch(userCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        long startTime = System.currentTimeMillis();

        try (ExecutorService executorService = Executors.newFixedThreadPool(10)) {
            for (int i = 0; i < userCount; i++) {
                final String userId = "user-" + i;
                executorService.submit(() -> {
                    try {
                        ticketService.reserveTicketWithRedissonLock(ticketId, userId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // then
        log.info("\n" + "=".repeat(80));
        log.info("[LV.3 Trade-off: 락 대기 시간(Latency)]");
        log.info("=".repeat(80));
        log.info("재고 10개에 50명이 동시 요청");
        log.info("실행 시간: {}ms (락 대기 시간 포함)", duration);
        log.info("성공: {}명, 실패: {}명", successCount.get(), failCount.get());
        log.info("평균 대기 시간: 약 {}ms/request", duration / userCount);
        log.info("=".repeat(80));
        log.info("💡 결론: 분산 락도 '락'이므로 대기 시간은 피할 수 없다.");
        log.info("   하지만 복잡한 비즈니스 로직의 정합성을 보장할 수 있다.");
        log.info("=".repeat(80));
    }

    private void printTestResult(String lockType, long duration, int successCount, int failCount,
                                 int finalStock, long reservationCount) {
        log.info("\n" + "=".repeat(80));
        log.info("[LV.3-{} 테스트 결과]", lockType);
        log.info("=".repeat(80));
        log.info("실행 시간: {}ms", duration);
        log.info("초기 재고: {}", INITIAL_STOCK);
        log.info("동시 요청 수: {}", CONCURRENT_USERS);
        log.info("성공 응답 수: {}", successCount);
        log.info("실패 응답 수: {}", failCount);
        log.info("최종 DB 재고: {}", finalStock);
        log.info("실제 예약 건수: {}", reservationCount);
        log.info("=".repeat(80));

        if (reservationCount == INITIAL_STOCK && finalStock == 0) {
            log.info("✅ 동시성 문제 해결 성공!");
            log.info("✅ 중복 구매 방지 + 재고 차감이 원자적으로 처리됨");
        } else {
            log.error("❌ 데이터 정합성 오류 발생!");
        }
    }
}
