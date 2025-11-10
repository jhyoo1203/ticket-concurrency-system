package com.ticket.lv2dblock.application.dto;

public record TicketResponse(
        Long id,
        String name,
        Integer stock,
        Long reservationCount
) {}
