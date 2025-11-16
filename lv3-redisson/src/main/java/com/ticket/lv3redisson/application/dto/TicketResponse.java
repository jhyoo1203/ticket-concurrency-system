package com.ticket.lv3redisson.application.dto;

public record TicketResponse(
        Long id,
        String name,
        Integer stock,
        Long reservationCount
) {}
