package com.ticket.lv3redisson.infrastructure;

import com.ticket.lv3redisson.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
