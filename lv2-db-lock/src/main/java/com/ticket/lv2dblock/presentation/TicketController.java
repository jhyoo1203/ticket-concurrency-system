package com.ticket.lv2dblock.presentation;

import com.ticket.lv2dblock.application.dto.TicketResponse;
import com.ticket.lv2dblock.domain.Ticket;
import com.ticket.lv2dblock.application.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/{ticketId}/reserve/synchronized")
    public ResponseEntity<String> reserveTicketWithSynchronized(
            @PathVariable Long ticketId,
            @RequestParam String userId) {

        ticketService.reserveTicketWithSynchronized(ticketId, userId);
        return ResponseEntity.ok("예매 성공 (Synchronized)");
    }

    @PostMapping("/{ticketId}/reserve/pessimistic")
    public ResponseEntity<String> reserveTicketWithPessimisticLock(
            @PathVariable Long ticketId,
            @RequestParam String userId) {

        ticketService.reserveTicketWithPessimisticLock(ticketId, userId);
        return ResponseEntity.ok("예매 성공 (Pessimistic Lock)");
    }

    @PostMapping("/{ticketId}/reserve/optimistic")
    public ResponseEntity<String> reserveTicketWithOptimisticLock(
            @PathVariable Long ticketId,
            @RequestParam String userId) {

        ticketService.reserveTicketWithOptimisticLockRetry(ticketId, userId, 5);
        return ResponseEntity.ok("예매 성공 (Optimistic Lock)");
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
