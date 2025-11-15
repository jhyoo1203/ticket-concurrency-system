package com.ticket.lv2dblock.application;

import com.ticket.lv2dblock.domain.Reservation;
import com.ticket.lv2dblock.domain.Ticket;
import com.ticket.lv2dblock.infrastructure.ReservationRepository;
import com.ticket.lv2dblock.infrastructure.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ReservationRepository reservationRepository;
    private final TicketReserveService ticketReserveService;

    /**
     * LV.2-1: Synchronized 키워드 사용
     *
     * 장점:
     * - 구현이 간단하고 직관적
     * - 단일 JVM 내에서는 동시성 문제 해결
     *
     * 단점:
     * - 단일 서버(JVM) 환경에서만 동작
     * - 멀티 인스턴스(분산 환경)에서는 동작하지 않음
     * - 모든 요청이 순차적으로 처리되어 성능 저하
     */
    @Transactional
    public synchronized void reserveTicketWithSynchronized(Long ticketId, String userId) {
        // 1. 티켓 조회
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("티켓을 찾을 수 없습니다."));

        // 2. 재고 확인
        if (!ticket.hasStock()) {
            throw new IllegalStateException("재고가 부족합니다.");
        }

        // 10ms 지연
        sleep(10);

        // 3. 재고 차감
        ticket.decreaseStock();
        ticketRepository.save(ticket);

        // 4. 예약 정보 저장
        Reservation reservation = new Reservation(ticketId, userId);
        reservationRepository.save(reservation);

        log.info("[Synchronized] 예약 완료 - 티켓 ID: {}, 사용자: {}, 남은 재고: {}", ticketId, userId, ticket.getStock());
    }

    /**
     * LV.2-2: Pessimistic Lock (비관적 락)
     *
     * 장점:
     * - 데이터 정합성 보장
     * - 멀티 인스턴스 환경에서도 동작
     * - 충돌이 많이 발생할 것으로 예상되는 경우 유리
     *
     * 단점:
     * - 락 대기 시간으로 인한 성능 저하 (병목)
     * - 데드락 발생 가능성
     * - DB에 부하 증가
     */
    @Transactional
    public void reserveTicketWithPessimisticLock(Long ticketId, String userId) {
        // 1. 티켓 조회 with Pessimistic Lock (SELECT ... FOR UPDATE)
        Ticket ticket = ticketRepository.findByIdWithPessimisticLock(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("티켓을 찾을 수 없습니다."));

        // 2. 재고 확인
        if (!ticket.hasStock()) {
            throw new IllegalStateException("재고가 부족합니다.");
        }

        // 10ms 지연
        sleep(10);

        // 3. 재고 차감
        ticket.decreaseStock();
        ticketRepository.save(ticket);

        // 4. 예약 정보 저장
        Reservation reservation = new Reservation(ticketId, userId);
        reservationRepository.save(reservation);

        log.info("[Pessimistic Lock] 예약 완료 - 티켓 ID: {}, 사용자: {}, 남은 재고: {}", ticketId, userId, ticket.getStock());
    }

    /**
     * LV.2-3: Optimistic Lock (낙관적 락)
     *
     * 장점:
     * - 락을 사용하지 않아 성능이 좋음
     * - 충돌이 적을 것으로 예상되는 경우 유리
     * - 멀티 인스턴스 환경에서도 동작
     *
     * 단점:
     * - 충돌 발생 시 재시도 로직 필요
     * - 충돌이 많은 경우 오히려 성능 저하
     * - 사용자에게 실패 경험 제공
     */
    public void reserveTicketWithOptimisticLockRetry(Long ticketId, String userId, int maxRetries) {
        int retryCount = 0;

        while (retryCount <= maxRetries) {
            try {
                ticketReserveService.reserveTicket(ticketId, userId);
                return; // 성공 시 리턴
            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount > maxRetries) {
                    log.error("[Optimistic Lock Retry] 재시도 횟수 초과: {}/{}", retryCount, maxRetries);
                    throw new IllegalStateException("재시도 횟수 초과: " + maxRetries);
                }
                log.warn("[Optimistic Lock Retry] 버전 충돌 발생 - 재시도 {}/{}", retryCount, maxRetries);
                sleep(10);
            } catch (IllegalStateException e) {
                // 재고 부족 등의 비즈니스 예외는 재시도하지 않음
                throw e;
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

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
