package com.edumerge.support.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "ticket_events",
    indexes = @Index(name = "idx_event_ticket", columnList = "ticket_id")
)
public class TicketEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "ticket_id", nullable = false)
    public Long ticketId;

    @Column(nullable = false, length = 80)
    public String actor;

    @Column(nullable = false, length = 30)
    public String action;

    @Column(nullable = false, length = 2000)
    public String detail;

    @Column(nullable = false)
    public Instant at;

    protected TicketEvent() {
    }

    public TicketEvent(
            Long ticketId,
            String actor,
            String action,
            String detail) {

        this.ticketId = ticketId;
        this.actor = actor;
        this.action = action;
        this.detail = detail;
        this.at = Instant.now();
    }
}