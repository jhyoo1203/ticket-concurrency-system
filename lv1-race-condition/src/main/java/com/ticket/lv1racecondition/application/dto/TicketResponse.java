package com.ticket.lv1racecondition.application.dto;

public record TicketResponse(
        Long id,
        String name,
        Integer stock,
        Long reservationCount
) {}
