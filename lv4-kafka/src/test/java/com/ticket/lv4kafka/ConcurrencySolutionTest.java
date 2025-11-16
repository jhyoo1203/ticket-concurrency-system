package com.ticket.lv4kafka;

import com.ticket.lv4kafka.domain.Ticket;
import com.ticket.lv4kafka.infrastructure.ReservationRepository;
import com.ticket.lv4kafka.infrastructure.TicketRepository;
import com.ticket.lv4kafka.application.TicketService;
import com.ticket.lv4kafka.application.ReservationProducer;
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
 * LV.4: Kafka 기반 비동기 예매 처리 테스트
 */
@SpringBootTest
class ConcurrencySolutionTest {

    @Autowired
    private ReservationProducer reservationProducer;

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
    @DisplayName("Kafka 비동기 처리 - 100개 티켓을 1000명이 동시 예매")
    void testKafkaAsyncReservation() throws InterruptedException {
        // given
        final int people = CONCURRENT_USERS;
        final CountDownLatch countDownLatch = new CountDownLatch(people);

        // when
        List<Thread> workers = Stream
                .generate(() -> new Thread(new KafkaReservationWorker(ticketId, countDownLatch)))
                .limit(people)
                .toList();
        workers.forEach(Thread::start);
        countDownLatch.await();

        // Kafka Consumer가 메시지를 처리할 때까지 폴링으로 대기 (최대 30초)
        waitForReservationProcessing(ticketId, INITIAL_STOCK, 30000);

        // then
        int finalStock = ticketService.getTicket(ticketId).getStock();
        long reservationCount = ticketService.getReservationCount(ticketId);

        assertEquals(0, finalStock);
        assertEquals(INITIAL_STOCK, reservationCount);
    }

    /**
     * Kafka Consumer가 메시지를 처리할 때까지 대기
     * 폴링 방식으로 예약 건수가 예상치에 도달했는지 확인
     */
    private void waitForReservationProcessing(Long ticketId, int expectedCount, long timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long elapsed = 0;

        while (elapsed < timeoutMs) {
            long currentCount = ticketService.getReservationCount(ticketId);
            int currentStock = ticketService.getTicket(ticketId).getStock();

            if (currentCount >= expectedCount || currentStock == 0) {
                return;
            }

            Thread.sleep(100);
            elapsed = System.currentTimeMillis() - startTime;
        }
    }

    /**
     * Kafka Reservation Worker
     */
    private class KafkaReservationWorker implements Runnable {
        private final Long ticketId;
        private final CountDownLatch countDownLatch;

        public KafkaReservationWorker(Long ticketId, CountDownLatch countDownLatch) {
            this.ticketId = ticketId;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            try {
                String userId = "user-" + Thread.currentThread().threadId();
                reservationProducer.sendReservationRequest(ticketId, userId);
            } catch (Exception e) {
                // 예외는 무시
            } finally {
                countDownLatch.countDown();
            }
        }
    }
}
