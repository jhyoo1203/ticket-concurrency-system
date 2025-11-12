package com.ticket.lv2dblock;

import com.ticket.lv2dblock.domain.Ticket;
import com.ticket.lv2dblock.infrastructure.ReservationRepository;
import com.ticket.lv2dblock.infrastructure.TicketRepository;
import com.ticket.lv2dblock.application.TicketService;
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
 * LV.2: 동시성 문제 해결 방안 비교 테스트
 *
 * 1. Synchronized 키워드
 * 2. Pessimistic Lock (비관적 락)
 * 3. Optimistic Lock (낙관적 락)
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
    @DisplayName("[LV.2-1 성공] Synchronized - 1000명이 동시에 100개 티켓 예매")
    void testSynchronized() throws InterruptedException {
        // given
        Ticket ticket = new Ticket("Synchronized 테스트 티켓", INITIAL_STOCK);
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
                        ticketService.reserveTicketWithSynchronized(ticketId, userId);
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
        Ticket result = ticketService.getTicket(ticketId);
        long reservationCount = ticketService.getReservationCount(ticketId);

        printTestResult("Synchronized", duration, successCount.get(), failCount.get(), result.getStock(), reservationCount);

        assertThat(result.getStock()).isEqualTo(0);
        assertThat(reservationCount).isEqualTo(INITIAL_STOCK);
    }

    @Test
    @DisplayName("[LV.2-2 성공] Pessimistic Lock - 1000명이 동시에 100개 티켓 예매")
    void testPessimisticLock() throws InterruptedException {
        // given
        Ticket ticket = new Ticket("Pessimistic Lock 테스트 티켓", INITIAL_STOCK);
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
                        ticketService.reserveTicketWithPessimisticLock(ticketId, userId);
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
        Ticket result = ticketService.getTicket(ticketId);
        long reservationCount = ticketService.getReservationCount(ticketId);

        printTestResult("Pessimistic Lock", duration, successCount.get(), failCount.get(), result.getStock(), reservationCount);

        assertThat(result.getStock()).isEqualTo(0);
        assertThat(reservationCount).isEqualTo(INITIAL_STOCK);
    }

    @Test
    @DisplayName("[LV.2-3 성공] Optimistic Lock - 1000명이 동시에 100개 티켓 예매")
    void testOptimisticLock() throws InterruptedException {
        // given
        Ticket ticket = new Ticket("Optimistic Lock 테스트 티켓", INITIAL_STOCK);
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
                        ticketService.reserveTicketWithOptimisticLockRetry(ticketId, userId, 5);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        if (failCount.get() < 5) { // 처음 5개만 로깅
                            log.error("[Optimistic Lock Test] 예외 발생: {}", e.getMessage(), e);
                        }
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
        Ticket result = ticketService.getTicket(ticketId);
        long reservationCount = ticketService.getReservationCount(ticketId);

        printTestResult("Optimistic Lock", duration, successCount.get(), failCount.get(), result.getStock(), reservationCount);

        assertThat(result.getStock()).isEqualTo(0);
        assertThat(reservationCount).isEqualTo(INITIAL_STOCK);
    }

    private void printTestResult(String lockType, long duration, int successCount, int failCount,
                                 int finalStock, long reservationCount) {
        log.info("\n" + "=".repeat(80));
        log.info("[LV.2-{} 테스트 결과]", lockType);
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
        } else {
            log.error("❌ 데이터 정합성 오류 발생!");
        }
    }
}
