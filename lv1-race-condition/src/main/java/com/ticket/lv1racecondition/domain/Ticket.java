package com.ticket.lv1racecondition.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer stock;

    public Ticket(String name, Integer stock) {
        this.name = name;
        this.stock = stock;
    }

    /**
     * 재고 차감 (Race Condition 발생 가능)
     * SELECT와 UPDATE 사이에 다른 스레드가 끼어들 수 있음
     */
    public void decreaseStock() {
        if (this.stock <= 0) {
            throw new IllegalStateException("재고가 부족합니다.");
        }
        this.stock--;
    }

    public boolean hasStock() {
        return this.stock > 0;
    }
}
