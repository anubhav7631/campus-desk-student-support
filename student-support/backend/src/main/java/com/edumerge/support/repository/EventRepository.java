package com.edumerge.support.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edumerge.support.entity.TicketEvent;

public interface EventRepository extends JpaRepository<TicketEvent, Long> {

    List<TicketEvent> findByTicketIdOrderByAtAscIdAsc(Long ticketId);
}