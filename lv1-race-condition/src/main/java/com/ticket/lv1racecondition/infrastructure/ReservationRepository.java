package com.ticket.lv1racecondition.infrastructure;

import com.ticket.lv1racecondition.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    long countByTicketId(Long ticketId);
}
