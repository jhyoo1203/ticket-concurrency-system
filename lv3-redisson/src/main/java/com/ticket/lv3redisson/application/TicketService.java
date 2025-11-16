package com.ticket.lv3redisson.application;

import com.ticket.lv3redisson.domain.Ticket;
import com.ticket.lv3redisson.infrastructure.ReservationRepository;
import com.ticket.lv3redisson.infrastructure.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private static final String LOCK_KEY_PREFIX = "TICKET_LOCK:";
    private static final long WAIT_TIME = 5L; // 락 획득 대기 시간 (초)
    private static final long LEASE_TIME = 10L; // 락 자동 해제 시간 (초)

    private final RedissonClient redissonClient;
    private final TicketRepository ticketRepository;
    private final ReservationRepository reservationRepository;
    private final TicketReserveService ticketReserveService;

    /**
     * LV.3: Redisson 분산 락
     *
     * 장점:
     * - 분산 환경(멀티 인스턴스)에서도 동작
     * - 복잡한 비즈니스 로직(중복 구매 확인 + 재고 차감)을 원자적으로 처리
     * - Redis를 통해 빠른 락 획득/해제
     * - DB 부하가 Pessimistic Lock보다 적음
     *
     * 단점:
     * - Redis 장애 시 시스템 전체 영향(SPoF)
     * - 락 대기 시간(Latency)은 여전히 존재
     * - Redisson 설정 및 관리 필요
     */
    public void reserveTicketWithRedissonLock(Long ticketId, String userId) {
        String lockKey = LOCK_KEY_PREFIX + ticketId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 1. 락 획득 시도 (waitTime 동안 대기, leaseTime 후 자동 해제)
            boolean acquired = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);

            if (!acquired) {
                log.error("[Redisson Lock] 락 획득 실패 - 티켓 ID: {}, 사용자: {}", ticketId, userId);
                throw new IllegalStateException("예매 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            log.info("[Redisson Lock] 락 획득 성공 - 티켓 ID: {}, 사용자: {}", ticketId, userId);

            // 2. 락을 획득한 후, 트랜잭션 내에서 비즈니스 로직 수행
            ticketReserveService.reserveTicket(ticketId, userId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Redisson Lock] 락 획득 중 인터럽트 발생 - 티켓 ID: {}", ticketId, e);
            throw new RuntimeException("예매 처리 중 오류가 발생했습니다.", e);
        } finally {
            // 3. finally에서 락 해제 보장
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[Redisson Lock] 락 해제 - 티켓 ID: {}", ticketId);
            }
        }
    }

    @Transactional(readOnly = true)
    public Ticket getTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("티켓을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public long getReservationCount(Long ticketId) {
        return reservationRepository.countByTicketId(ticketId);
    }
}
