package com.edumerge.support.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edumerge.support.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStudentUsernameOrderByCreatedAtDesc(String username);

    List<Ticket> findAllByOrderByCreatedAtDesc();

    List<Ticket> findByStatusNotAndEscalatedFalseAndDueAtBefore(
        String status,
        Instant now
    );
}