package com.ticket.lv3redisson.infrastructure;

import com.ticket.lv3redisson.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    long countByTicketId(Long ticketId);
    boolean existsByTicketIdAndUserId(Long ticketId, String userId);
}
