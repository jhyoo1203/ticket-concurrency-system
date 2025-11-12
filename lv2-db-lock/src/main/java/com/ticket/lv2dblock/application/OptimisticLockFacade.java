package com.ticket.lv2dblock.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ticket.lv2dblock.domain.Reservation;
import com.ticket.lv2dblock.domain.Ticket;
import com.ticket.lv2dblock.infrastructure.ReservationRepository;
import com.ticket.lv2dblock.infrastructure.TicketRepository;

/**
 * Optimistic Lock용 트랜잭션 Facade
 * Self-invocation 문제를 해결하기 위한 별도 컴포넌트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OptimisticLockFacade {

    private final TicketRepository ticketRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserveTicket(Long ticketId, String userId) {
        // 1. 티켓 조회 with Optimistic Lock
        Ticket ticket = ticketRepository.findByIdWithOptimisticLock(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("티켓을 찾을 수 없습니다."));

        // 2. 재고 확인
        if (!ticket.hasStock()) {
            throw new IllegalStateException("재고가 부족합니다.");
        }

        // 10ms 지연
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 3. 재고 차감
        ticket.decreaseStock();
        ticketRepository.save(ticket); // version 충돌 시 ObjectOptimisticLockingFailureException 발생

        // 4. 예약 정보 저장
        Reservation reservation = new Reservation(ticketId, userId);
        reservationRepository.save(reservation);

        log.info("[Optimistic Lock] 예약 완료 - 티켓 ID: {}, 사용자: {}, 남은 재고: {}", ticketId, userId, ticket.getStock());
    }
}
