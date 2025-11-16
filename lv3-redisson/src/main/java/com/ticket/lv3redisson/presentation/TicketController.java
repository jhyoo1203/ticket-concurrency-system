package com.ticket.lv3redisson.presentation;

import com.ticket.lv3redisson.application.dto.TicketResponse;
import com.ticket.lv3redisson.domain.Ticket;
import com.ticket.lv3redisson.application.TicketService;
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

        ticketService.reserveTicketWithRedissonLock(ticketId, userId);
        return ResponseEntity.ok("예매 성공 (Redisson Lock)");
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
