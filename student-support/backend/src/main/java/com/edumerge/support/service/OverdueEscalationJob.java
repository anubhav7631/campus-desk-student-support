package com.edumerge.support.service;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.edumerge.support.entity.Ticket;
import com.edumerge.support.entity.TicketEvent;
import com.edumerge.support.repository.EventRepository;
import com.edumerge.support.repository.TicketRepository;

@Component
public class OverdueEscalationJob {

    private final TicketRepository tickets;

    private final EventRepository events;

    private final ManagerEmailService email;

    public OverdueEscalationJob(
            TicketRepository tickets,
            EventRepository events,
            ManagerEmailService email) {

        this.tickets = tickets;
        this.events = events;
        this.email = email;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void escalateOverdueTickets() {

        var overdue = tickets
            .findByStatusNotAndEscalatedFalseAndDueAtBefore(
                "RESOLVED",
                Instant.now()
            );

        for (Ticket ticket : overdue) {

            ticket.escalated = true;
            ticket.updatedAt = Instant.now();
            tickets.save(ticket);

            events.save(new TicketEvent(
                ticket.id,
                "system",
                "ESCALATE",
                "Automatically escalated because the SLA deadline passed"
            ));

            try {
                email.sendEscalation(ticket);
            } catch (Exception ex) {
                System.err.println(
                    "Could not email manager for ticket #" +
                    ticket.id + ": " + ex.getMessage()
                );
            }
        }
    }
}