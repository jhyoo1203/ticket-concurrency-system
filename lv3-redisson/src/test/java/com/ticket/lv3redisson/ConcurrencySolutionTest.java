package com.ticket.lv3redisson;

import com.ticket.lv3redisson.domain.Ticket;
import com.ticket.lv3redisson.infrastructure.ReservationRepository;
import com.ticket.lv3redisson.infrastructure.TicketRepository;
import com.ticket.lv3redisson.application.TicketService;
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
 * LV.3: Redisson 분산 락을 이용한 동시성 문제 해결 테스트
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
    @DisplayName("Redisson 분산 락 - 100개 티켓을 1000명이 동시 예매")
    void testRedissonDistributedLock() throws InterruptedException {
        // given
        final int people = CONCURRENT_USERS;
        final CountDownLatch countDownLatch = new CountDownLatch(people);

        // when
        List<Thread> workers = Stream
                .generate(() -> new Thread(new RedissonLockWorker(ticketId, countDownLatch)))
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
    @DisplayName("Redisson 분산 락 - 10개 티켓을 50명이 동시 예매 (락 대기 시간 확인)")
    void testRedissonLockLatency() throws InterruptedException {
        // given
        reservationRepository.deleteAll();
        ticketRepository.deleteAll();
        Ticket ticket = ticketRepository.save(new Ticket("Latency 테스트 티켓", 10));
        Long testTicketId = ticket.getId();

        final int people = 50;
        final CountDownLatch countDownLatch = new CountDownLatch(people);

        // when
        long startTime = System.currentTimeMillis();
        List<Thread> workers = Stream
                .generate(() -> new Thread(new RedissonLockWorker(testTicketId, countDownLatch)))
                .limit(people)
                .toList();
        workers.forEach(Thread::start);
        countDownLatch.await();
        long duration = System.currentTimeMillis() - startTime;

        // then
        int finalStock = ticketService.getTicket(testTicketId).getStock();
        long reservationCount = ticketService.getReservationCount(testTicketId);

        assertEquals(0, finalStock);
        assertEquals(10, reservationCount);
    }

    /**
     * Redisson Lock Worker
     */
    private class RedissonLockWorker implements Runnable {
        private final Long ticketId;
        private final CountDownLatch countDownLatch;

        public RedissonLockWorker(Long ticketId, CountDownLatch countDownLatch) {
            this.ticketId = ticketId;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            try {
                String userId = "user-" + Thread.currentThread().threadId();
                ticketService.reserveTicketWithRedissonLock(ticketId, userId);
            } catch (Exception e) {
                // 재고 부족 등의 예외는 무시
            } finally {
                countDownLatch.countDown();
            }
        }
    }
}
