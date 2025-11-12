package com.ticket.lv1racecondition;

import com.ticket.lv1racecondition.domain.Ticket;
import com.ticket.lv1racecondition.infrastructure.ReservationRepository;
import com.ticket.lv1racecondition.infrastructure.TicketRepository;
import com.ticket.lv1racecondition.application.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LV.1: Race Condition 테스트
 *
 * 목표: 동시성 문제를 재현하고 증명한다
 * - 100개의 재고에 1000명이 동시에 요청
 * - 락이 없으므로 100개 이상의 예약이 발생할 것으로 예상
 */
@SpringBootTest
class ConcurrencyProblemTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Long ticketId;
    private static final int INITIAL_STOCK = 100;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        ticketRepository.deleteAll();

        Ticket ticket = new Ticket("테스트 콘서트 티켓", INITIAL_STOCK);
        this.ticketId = ticketRepository.save(ticket).getId();
    }

    @Test
    @DisplayName("초기 재고 확인")
    void checkInitialStock() {
        // given & when
        int currentStock = ticketService.getTicket(ticketId).getStock();

        // then
        assertEquals(INITIAL_STOCK, currentStock);
    }

    @Test
    @DisplayName("순차 실행 - 10명이 10개 티켓 예매")
    void sequentialReservation_shouldWorkCorrectly() {
        // given
        reservationRepository.deleteAll();
        ticketRepository.deleteAll();
        Ticket ticket = ticketRepository.save(new Ticket("순차 테스트", 10));

        // when
        for (int i = 0; i < 10; i++) {
            ticketService.reserveTicket(ticket.getId(), "user-" + i);
        }

        // then
        int finalStock = ticketService.getTicket(ticket.getId()).getStock();
        long reservationCount = ticketService.getReservationCount(ticket.getId());

        assertEquals(0, finalStock);
        assertEquals(10, reservationCount);
    }

    @Test
    @DisplayName("락 X - 100개 티켓을 1000명이 동시 예매 (Race Condition 발생)")
    void concurrentReservation_withoutLock_shouldCauseRaceCondition() throws InterruptedException {
        // given
        final int people = 1000;
        final CountDownLatch countDownLatch = new CountDownLatch(people);

        // when
        List<Thread> workers = Stream
                .generate(() -> new Thread(new ReservationWorker(ticketId, "user", countDownLatch)))
                .limit(people)
                .toList();
        workers.forEach(Thread::start);
        countDownLatch.await();

        // then
        int finalStock = ticketService.getTicket(ticketId).getStock();
        long reservationCount = ticketService.getReservationCount(ticketId);

        // Race Condition 발생으로 초과 예약이 발생함
        assertTrue(reservationCount > INITIAL_STOCK);
    }

    /**
     * 티켓 예매 Worker
     */
    private class ReservationWorker implements Runnable {
        private final Long ticketId;
        private final String userIdPrefix;
        private final CountDownLatch countDownLatch;

        public ReservationWorker(Long ticketId, String userIdPrefix, CountDownLatch countDownLatch) {
            this.ticketId = ticketId;
            this.userIdPrefix = userIdPrefix;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            try {
                String userId = userIdPrefix + "-" + Thread.currentThread().threadId();
                ticketService.reserveTicket(ticketId, userId);
            } catch (Exception e) {
                // 재고 부족 등의 예외는 무시
            } finally {
                countDownLatch.countDown();
            }
        }
    }
}

