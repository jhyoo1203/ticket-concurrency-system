package com.ticket.lv4kafka.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketReserveService {

    private static final String LOCK_KEY_PREFIX = "TICKET_LOCK:";
    private static final long WAIT_TIME = 5L;
    private static final long LEASE_TIME = 10L;

    private final RedissonClient redissonClient;
    private final TicketService ticketService;

    /**
     * Kafka Consumer가 메시지를 받아서 실제 예매 처리를 수행하는 메서드
     * Redisson Lock을 사용하여 동시성 문제 해결
     */
    public void processReservation(Long ticketId, String userId) {
        String lockKey = LOCK_KEY_PREFIX + ticketId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);

            if (!acquired) {
                log.error("[Kafka Consumer] 락 획득 실패 - 티켓 ID: {}, 사용자: {}", ticketId, userId);
                throw new IllegalStateException("예매 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            log.info("[Kafka Consumer] 락 획득 성공 - 티켓 ID: {}, 사용자: {}", ticketId, userId);

            // 트랜잭션 내에서 비즈니스 로직 수행
            ticketService.reserveTicket(ticketId, userId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Kafka Consumer] 락 획득 중 인터럽트 발생 - 티켓 ID: {}", ticketId, e);
            throw new RuntimeException("예매 처리 중 오류가 발생했습니다.", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[Kafka Consumer] 락 해제 - 티켓 ID: {}", ticketId);
            }
        }
    }
}
