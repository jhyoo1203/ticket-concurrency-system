package com.ticket.lv1racecondition.application;

import com.ticket.lv1racecondition.domain.Reservation;
import com.ticket.lv1racecondition.domain.Ticket;
import com.ticket.lv1racecondition.infrastructure.ReservationRepository;
import com.ticket.lv1racecondition.infrastructure.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ReservationRepository reservationRepository;

    /**
     * LV.1: Race Condition 발생
     *
     * 문제점:
     * 1. SELECT로 재고를 확인한다 (hasStock())
     * 2. UPDATE로 재고를 차감한다 (decreaseStock())
     * 3. 1번과 2번 사이의 시간차(Time-of-check to time-of-use)로 인해
     *    여러 스레드가 동시에 진입하면 재고가 정확히 차감되지 않음
     */
    @Transactional
    public void reserveTicket(Long ticketId, String userId) {
        // 1. 티켓 조회 (SELECT)
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("티켓을 찾을 수 없습니다."));

        // 2. 재고 확인
        if (!ticket.hasStock()) {
            throw new IllegalStateException("재고가 부족합니다.");
        }

        // 10초 지연
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 3. 재고 차감 (UPDATE)
        ticket.decreaseStock();
        ticketRepository.save(ticket);

        // 4. 예약 정보 저장
        Reservation reservation = new Reservation(ticketId, userId);
        reservationRepository.save(reservation);

        log.info("예약 완료 - 티켓 ID: {}, 사용자: {}, 남은 재고: {}", ticketId, userId, ticket.getStock());
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
