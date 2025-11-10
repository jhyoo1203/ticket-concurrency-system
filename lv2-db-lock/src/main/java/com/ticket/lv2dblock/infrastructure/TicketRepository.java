package com.ticket.lv2dblock.infrastructure;

import com.ticket.lv2dblock.domain.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Pessimistic Lock (비관적 락)
     * SELECT ... FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findByIdWithPessimisticLock(@Param("id") Long id);

    /**
     * Optimistic Lock (낙관적 락)
     * @Version을 이용한 버전 관리
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findByIdWithOptimisticLock(@Param("id") Long id);
}
