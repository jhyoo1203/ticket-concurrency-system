package com.ticket.lv1racecondition.infrastructure;

import com.ticket.lv1racecondition.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
