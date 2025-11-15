package com.ticket.lv4kafka.application;

import com.ticket.lv4kafka.application.dto.ReservationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * LV.4: Kafka Consumer
 * Kafka에서 메시지를 소비하여 실제 예매 처리를 수행하는 역할
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationConsumer {

    private final TicketReserveService ticketReserveService;

    /**
     * Kafka에서 예매 요청 메시지를 받아서 처리
     *
     * @param request 예매 요청 정보
     */
    @KafkaListener(topics = "ticket-reservation", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeReservationRequest(ReservationRequest request) {
        log.info("[Kafka Consumer] 예매 요청 수신 - 티켓 ID: {}, 사용자: {}", request.getTicketId(), request.getUserId());

        try {
            // Redisson Lock을 이용한 예매 처리
            ticketReserveService.processReservation(request.getTicketId(), request.getUserId());
            log.info("[Kafka Consumer] 예매 처리 완료 - 티켓 ID: {}, 사용자: {}", request.getTicketId(), request.getUserId());
        } catch (Exception e) {
            log.error("[Kafka Consumer] 예매 처리 실패 - 티켓 ID: {}, 사용자: {}, 에러: {}",
                    request.getTicketId(), request.getUserId(), e.getMessage());
            // TODO: 실패한 메시지를 DLQ로 보내거나 재시도 로직 추가 가능
        }
    }
}
