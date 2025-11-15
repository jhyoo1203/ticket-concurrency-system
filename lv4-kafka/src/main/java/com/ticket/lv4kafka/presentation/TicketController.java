package com.ticket.lv4kafka.presentation;

import com.ticket.lv4kafka.application.TicketService;
import com.ticket.lv4kafka.application.dto.TicketResponse;
import com.ticket.lv4kafka.domain.Ticket;
import com.ticket.lv4kafka.application.ReservationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * LV.4: Kafka 기반 비동기 예매 API
 */
@Slf4j
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final ReservationProducer reservationProducer;

    /**
     * LV.4: Kafka를 이용한 비동기 예매 요청
     *
     * 장점:
     * - 사용자에게 즉시 응답 (Response Time 극단적으로 단축)
     * - 트래픽 스파이크 대응 가능 (큐에 쌓아두고 천천히 처리)
     * - 시스템 안정성 확보 (부하 제어)
     *
     * 단점:
     * - 아키텍처 복잡도 증가
     * - 최종 일관성(Eventual Consistency) 모델
     * - Kafka 인프라 관리 필요
     *
     * @param ticketId 티켓 ID
     * @param userId 사용자 ID
     * @return 예매 접수 완료 메시지
     */
    @PostMapping("/{ticketId}/reserve")
    public ResponseEntity<String> reserveTicket(
            @PathVariable Long ticketId,
            @RequestParam String userId) {

        log.info("[API] 예매 요청 수신 - 티켓 ID: {}, 사용자: {}", ticketId, userId);

        // Kafka에 메시지를 발행하고 즉시 응답
        reservationProducer.sendReservationRequest(ticketId, userId);

        return ResponseEntity.ok("예매 접수가 완료되었습니다. 처리 결과는 곧 알려드리겠습니다. (Kafka Async)");
    }

    /**
     * 티켓 정보 조회
     */
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
