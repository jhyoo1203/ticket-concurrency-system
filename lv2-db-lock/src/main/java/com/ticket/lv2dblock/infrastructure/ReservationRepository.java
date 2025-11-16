package com.ticket.lv2dblock.infrastructure;

import com.ticket.lv2dblock.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    long countByTicketId(Long ticketId);
}
