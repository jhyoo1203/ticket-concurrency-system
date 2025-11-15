package com.ticket.lv4kafka.infrastructure;

import com.ticket.lv4kafka.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
