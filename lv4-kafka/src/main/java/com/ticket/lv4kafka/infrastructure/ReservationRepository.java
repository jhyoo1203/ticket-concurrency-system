package com.ticket.lv4kafka.infrastructure;

import com.ticket.lv4kafka.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    long countByTicketId(Long ticketId);
    boolean existsByTicketIdAndUserId(Long ticketId, String userId);
}
