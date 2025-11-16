package com.ticket.lv4kafka.application.dto;

public record TicketResponse(
        Long id,
        String name,
        Integer stock,
        Long reservationCount
) {}
