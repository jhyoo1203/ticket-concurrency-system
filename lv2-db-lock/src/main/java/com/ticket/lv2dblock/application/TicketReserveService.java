package com.ticket.lv2dblock.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ticket.lv2dblock.domain.Reservation;
import com.ticket.lv2dblock.domain.Ticket;
import com.ticket.lv2dblock.infrastructure.ReservationRepository;
import com.ticket.lv2dblock.infrastructure.TicketRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketReserveService {

    private final TicketRepository ticketRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
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
