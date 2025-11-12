package com.ticket.lv3redisson.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ticketId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private LocalDateTime reservedAt;

    public Reservation(Long ticketId, String userId) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.reservedAt = LocalDateTime.now();
    }
}
