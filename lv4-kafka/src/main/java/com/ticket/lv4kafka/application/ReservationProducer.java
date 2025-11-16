package com.ticket.lv4kafka.application;

import com.ticket.lv4kafka.application.dto.ReservationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * LV.4: Kafka Producer
 * API 요청을 받아 Kafka에 메시지를 발행하는 역할
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationProducer {

    private static final String TOPIC_NAME = "ticket-reservation";

    private final KafkaTemplate<String, ReservationRequest> kafkaTemplate;

    /**
     * 예매 요청을 Kafka에 발행
     *
     * @param ticketId 티켓 ID
     * @param userId 사용자 ID
     */
    public void sendReservationRequest(Long ticketId, String userId) {
        ReservationRequest request = new ReservationRequest(ticketId, userId);

        kafkaTemplate.send(TOPIC_NAME, String.valueOf(ticketId), request)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[Kafka Producer] 예매 요청 발행 성공 - 티켓 ID: {}, 사용자: {}", ticketId, userId);
                    } else {
                        log.error("[Kafka Producer] 예매 요청 발행 실패 - 티켓 ID: {}, 사용자: {}, errorMsg: {}", ticketId, userId,
                                ex.getMessage());
                    }
                });
    }
}
