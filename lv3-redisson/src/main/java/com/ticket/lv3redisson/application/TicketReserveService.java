package com.ticket.lv3redisson.application;

import com.ticket.lv3redisson.domain.Reservation;
import com.ticket.lv3redisson.domain.Ticket;
import com.ticket.lv3redisson.infrastructure.ReservationRepository;
import com.ticket.lv3redisson.infrastructure.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketReserveService {

    private final TicketRepository ticketRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public void reserveTicket(Long ticketId, String userId) {
        // 1. 티켓 조회
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("티켓을 찾을 수 없습니다."));

        // 2. 중복 구매 확인
        boolean alreadyReserved = reservationRepository.existsByTicketIdAndUserId(ticketId, userId);
        if (alreadyReserved) {
            log.warn("[Redisson Lock] 중복 예매 시도 - 티켓 ID: {}, 사용자: {}", ticketId, userId);
            throw new IllegalStateException("이미 예매한 티켓입니다.");
        }

        // 3. 재고 확인
        if (!ticket.hasStock()) {
            log.warn("[Redisson Lock] 재고 부족 - 티켓 ID: {}, 사용자: {}", ticketId, userId);
            throw new IllegalStateException("재고가 부족합니다.");
        }

        // 10ms 지연
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 4. 재고 차감
        ticket.decreaseStock();
        ticketRepository.save(ticket);

        // 5. 예약 정보 저장
        Reservation reservation = new Reservation(ticketId, userId);
        reservationRepository.save(reservation);

        log.info("[Redisson Lock] 예약 완료 - 티켓 ID: {}, 사용자: {}, 남은 재고: {}", ticketId, userId, ticket.getStock());
    }
}
