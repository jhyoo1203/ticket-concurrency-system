package com.ticket.lv2dblock;

import com.ticket.lv2dblock.domain.Ticket;
import com.ticket.lv2dblock.infrastructure.ReservationRepository;
import com.ticket.lv2dblock.infrastructure.TicketRepository;
import com.ticket.lv2dblock.application.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * LV.2: 동시성 문제 해결 방안 비교 테스트
 *
 * 1. Synchronized 키워드
 * 2. Pessimistic Lock (비관적 락)
 * 3. Optimistic Lock (낙관적 락)
 */
@SpringBootTest
class ConcurrencySolutionTest {

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
        reservationRepository.deleteAll();
        ticketRepository.deleteAll();

        Ticket ticket = new Ticket("테스트 콘서트 티켓", INITIAL_STOCK);
        this.ticketId = ticketRepository.save(ticket).getId();
    }

    @Test
    @DisplayName("Synchronized - 100개 티켓을 1000명이 동시 예매")
    void testSynchronized() throws InterruptedException {
        // given
        final int people = CONCURRENT_USERS;
        final CountDownLatch countDownLatch = new CountDownLatch(people);

        // when
        List<Thread> workers = Stream
                .generate(() -> new Thread(new SynchronizedWorker(ticketId, countDownLatch)))
                .limit(people)
                .toList();
        workers.forEach(Thread::start);
        countDownLatch.await();

        // then
        int finalStock = ticketService.getTicket(ticketId).getStock();
        long reservationCount = ticketService.getReservationCount(ticketId);

        assertEquals(0, finalStock);
        assertEquals(INITIAL_STOCK, reservationCount);
    }

    @Test
    @DisplayName("Pessimistic Lock - 100개 티켓을 1000명이 동시 예매")
    void testPessimisticLock() throws InterruptedException {
        // given
        final int people = CONCURRENT_USERS;
        final CountDownLatch countDownLatch = new CountDownLatch(people);

        // when
        List<Thread> workers = Stream
                .generate(() -> new Thread(new PessimisticLockWorker(ticketId, countDownLatch)))
                .limit(people)
                .toList();
        workers.forEach(Thread::start);
        countDownLatch.await();

        // then
        int finalStock = ticketService.getTicket(ticketId).getStock();
        long reservationCount = ticketService.getReservationCount(ticketId);

        assertEquals(0, finalStock);
        assertEquals(INITIAL_STOCK, reservationCount);
    }

    @Test
    @DisplayName("Optimistic Lock - 100개 티켓을 1000명이 동시 예매")
    void testOptimisticLock() throws InterruptedException {
        // given
        final int people = CONCURRENT_USERS;
        final CountDownLatch countDownLatch = new CountDownLatch(people);

        // when
        List<Thread> workers = Stream
                .generate(() -> new Thread(new OptimisticLockWorker(ticketId, countDownLatch)))
                .limit(people)
                .toList();
        workers.forEach(Thread::start);
        countDownLatch.await();

        // then
        int finalStock = ticketService.getTicket(ticketId).getStock();
        long reservationCount = ticketService.getReservationCount(ticketId);

        assertEquals(0, finalStock);
        assertEquals(INITIAL_STOCK, reservationCount);
    }

    /**
     * Synchronized Worker
     */
    private class SynchronizedWorker implements Runnable {
        private final Long ticketId;
        private final CountDownLatch countDownLatch;

        public SynchronizedWorker(Long ticketId, CountDownLatch countDownLatch) {
            this.ticketId = ticketId;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            try {
                String userId = "user-" + Thread.currentThread().threadId();
                ticketService.reserveTicketWithSynchronized(ticketId, userId);
            } catch (Exception e) {
                // 재고 부족 등의 예외는 무시
            } finally {
                countDownLatch.countDown();
            }
        }
    }

    /**
     * Pessimistic Lock Worker
     */
    private class PessimisticLockWorker implements Runnable {
        private final Long ticketId;
        private final CountDownLatch countDownLatch;

        public PessimisticLockWorker(Long ticketId, CountDownLatch countDownLatch) {
            this.ticketId = ticketId;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            try {
                String userId = "user-" + Thread.currentThread().threadId();
                ticketService.reserveTicketWithPessimisticLock(ticketId, userId);
            } catch (Exception e) {
                // 재고 부족 등의 예외는 무시
            } finally {
                countDownLatch.countDown();
            }
        }
    }

    /**
     * Optimistic Lock Worker
     */
    private class OptimisticLockWorker implements Runnable {
        private final Long ticketId;
        private final CountDownLatch countDownLatch;

        public OptimisticLockWorker(Long ticketId, CountDownLatch countDownLatch) {
            this.ticketId = ticketId;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            try {
                String userId = "user-" + Thread.currentThread().threadId();
                ticketService.reserveTicketWithOptimisticLockRetry(ticketId, userId, 5);
            } catch (Exception e) {
                // 재고 부족 등의 예외는 무시
            } finally {
                countDownLatch.countDown();
            }
        }
    }
}
