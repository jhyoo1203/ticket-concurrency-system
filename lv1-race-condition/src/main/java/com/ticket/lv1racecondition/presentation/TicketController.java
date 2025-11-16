package com.ticket.lv1racecondition.presentation;

import com.ticket.lv1racecondition.application.dto.TicketResponse;
import com.ticket.lv1racecondition.domain.Ticket;
import com.ticket.lv1racecondition.application.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/{ticketId}/reserve")
    public ResponseEntity<String> reserveTicket(
            @PathVariable Long ticketId,
            @RequestParam String userId) {

        ticketService.reserveTicket(ticketId, userId);
        return ResponseEntity.ok("예매 성공");
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable Long ticketId) {
        Ticket ticket = ticketService.getTicket(ticketId);
        long reservationCount = ticketService.getReservationCount(ticketId);

        return ResponseEntity.ok(new TicketResponse(
                ticket.getId(),
                ticket.getName(),
                ticket.getStock(),
                reservationCount
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
