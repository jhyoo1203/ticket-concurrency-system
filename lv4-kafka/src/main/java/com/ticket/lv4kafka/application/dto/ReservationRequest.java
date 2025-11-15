package com.ticket.lv4kafka.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka 메시지로 전송할 예매 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    private Long ticketId;
    private String userId;
}
