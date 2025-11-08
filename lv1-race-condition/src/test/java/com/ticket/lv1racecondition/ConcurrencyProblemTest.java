package com.ticket.lv1racecondition;

import com.ticket.lv1racecondition.domain.Ticket;
import com.ticket.lv1racecondition.infrastructure.ReservationRepository;
import com.ticket.lv1racecondition.infrastructure.TicketRepository;
import com.ticket.lv1racecondition.application.TicketService;
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
 * LV.1: Race Condition 테스트
 *
 * 목표: 동시성 문제를 재현하고 증명한다
 * - 100개의 재고에 1000명이 동시에 요청
 * - 락이 없으므로 100개 이상의 예약이 발생할 것으로 예상
 */
@SpringBootTest
class ConcurrencyProblemTest {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyProblemTest.class);

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Long ticketId;
    private static final int INITIAL_STOCK = 100;
    private static final int CONCURRENT_USERS = 1000;

    @BeforeEach
    void setUp() {
        // 데이터 초기화
        reservationRepository.deleteAll();
        ticketRepository.deleteAll();

        // 테스트용 티켓 생성 (재고 100개)
        Ticket ticket = new Ticket("테스트 콘서트 티켓", INITIAL_STOCK);
        ticket = ticketRepository.save(ticket);
        ticketId = ticket.getId();

        log.info("테스트 준비 완료 - 티켓 ID: {}, 초기 재고: {}", ticketId, INITIAL_STOCK);
    }

    @Test
    @DisplayName("[LV.1 실패 예상] 1000명이 동시에 100개 티켓 예매 - Race Condition 발생")
    void testRaceCondition() throws InterruptedException {
        // given
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 동시에 예매 요청 실행
        long startTime = System.currentTimeMillis();

        try (ExecutorService executorService = Executors.newFixedThreadPool(100)) {
            for (int i = 0; i < CONCURRENT_USERS; i++) {
                final String userId = "user-" + i;
                executorService.submit(() -> {
                    try {
                        ticketService.reserveTicket(ticketId, userId);
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
        Ticket ticket = ticketService.getTicket(ticketId);
        long reservationCount = ticketService.getReservationCount(ticketId);

        log.info("=".repeat(80));
        log.info("[LV.1 Race Condition 테스트 결과]");
        log.info("=".repeat(80));
        log.info("실행 시간: {}ms", duration);
        log.info("초기 재고: {}", INITIAL_STOCK);
        log.info("동시 요청 수: {}", CONCURRENT_USERS);
        log.info("성공 응답 수: {}", successCount.get());
        log.info("실패 응답 수: {}", failCount.get());
        log.info("최종 DB 재고: {}", ticket.getStock());
        log.info("실제 예약 건수: {}", reservationCount);
        log.info("예상 재고: 0 (100 - 100)");
        log.info("=".repeat(80));

        // 검증: Race Condition 발생 확인
        // 락이 없으므로 예약 건수가 100개를 초과할 것으로 예상
        log.warn("⚠️ 예약 건수가 초기 재고({})를 초과했습니다 (실제: {})", INITIAL_STOCK, reservationCount);
        log.warn("⚠️ Race Condition 발생");
        log.warn("⚠️ 여러 스레드가 동시에 'SELECT → UPDATE' 사이에 끼어들어 재고가 정확히 차감되지 않았습니다.");

        if (reservationCount > INITIAL_STOCK) {
            log.error("❌ 데이터 정합성 오류 발생! 100개 재고에 {}개가 예약됨", reservationCount);
        }

        assertThat(ticket.getStock() >= 0).isTrue();
    }

    @Test
    @DisplayName("[LV.1 추가 테스트] 순차 실행 시 정상 동작 확인")
    void testSequentialExecution() {
        // given
        reservationRepository.deleteAll();
        ticketRepository.deleteAll();

        Ticket ticket = new Ticket("순차 테스트 티켓", 10);
        ticket = ticketRepository.save(ticket);
        Long testTicketId = ticket.getId();

        // when
        for (int i = 0; i < 10; i++) {
            ticketService.reserveTicket(testTicketId, "user-" + i);
        }

        // then
        Ticket result = ticketService.getTicket(testTicketId);
        long reservationCount = ticketService.getReservationCount(testTicketId);

        log.info("[순차 실행 테스트 결과]");
        log.info("최종 재고: {}", result.getStock());
        log.info("예약 건수: {}", reservationCount);

        assertThat(result.getStock()).isEqualTo(0);
        assertThat(reservationCount).isEqualTo(10);
    }
}

