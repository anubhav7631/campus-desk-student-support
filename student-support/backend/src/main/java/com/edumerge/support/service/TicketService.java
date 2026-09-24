package com.edumerge.support.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.edumerge.support.entity.Ticket;
import com.edumerge.support.entity.TicketEvent;
import com.edumerge.support.repository.EventRepository;
import com.edumerge.support.repository.TicketRepository;

@Service
public class TicketService {

    private final TicketRepository tickets;
    private final EventRepository events;

    public TicketService(TicketRepository tickets, EventRepository events) {
        this.tickets = tickets;
        this.events = events;
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private void require(boolean allowed) {
        if (!allowed) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Not permitted for this role"
            );
        }
    }

    private void requireInput(boolean valid, String message) {
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    public Ticket get(Long id, Authentication auth) {
        Ticket ticket = tickets.findById(id).orElseThrow(
            () -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Ticket not found"
            )
        );

        if (hasRole(auth, "STUDENT")) {
            require(ticket.studentUsername.equals(auth.getName()));
        }

        return ticket;
    }

    public List<Ticket> list(Authentication auth) {
        if (hasRole(auth, "STUDENT")) {
            return tickets.findByStudentUsernameOrderByCreatedAtDesc(
                auth.getName()
            );
        }

        return tickets.findAllByOrderByCreatedAtDesc();
    }

    public List<TicketEvent> history(Long id, Authentication auth) {
        get(id, auth);
        return events.findByTicketIdOrderByAtAscIdAsc(id);
    }

    @Transactional
    public Ticket create(
            String title,
            String description,
            String categoryInput,
            String priorityInput,
            Authentication auth) {

        require(hasRole(auth, "STUDENT"));

        String category = categoryInput.trim().toUpperCase();
        String priority = priorityInput.trim().toUpperCase();

        requireInput(
            List.of("FEES", "ATTENDANCE", "ID_CARD", "DOCUMENTS", "OTHER")
                .contains(category),
            "Invalid category"
        );

        requireInput(
            List.of("LOW", "MEDIUM", "HIGH").contains(priority),
            "Invalid priority"
        );

        Ticket ticket = new Ticket();
        ticket.studentUsername = auth.getName();
        ticket.title = title.trim();
        ticket.description = description.trim();
        ticket.category = category;
        ticket.priority = priority;
        ticket.status = "OPEN";
        ticket.createdAt = Instant.now();
        ticket.updatedAt = ticket.createdAt;
        ticket.dueAt = ticket.createdAt.plus(
            slaHours(priority), ChronoUnit.HOURS
        );

        tickets.save(ticket);

        events.save(new TicketEvent(
            ticket.id,
            auth.getName(),
            "CREATED",
            "New " + category + " ticket, " + priority + " priority"
        ));

        return ticket;
    }

    private long slaHours(String priority) {
        return switch (priority) {
            case "HIGH" -> 24;
            case "MEDIUM" -> 48;
            default -> 72;
        };
    }

    @Transactional
    public Ticket change(
            Long id,
            String actionInput,
            String valueInput,
            String note,
            Authentication auth) {

        Ticket ticket = get(id, auth);
        String action = actionInput.trim().toUpperCase();
        String value = valueInput == null ? "" : valueInput.trim();
        String before = ticket.status;

        switch (action) {
            case "ASSIGN" -> {
                require(
                    hasRole(auth, "MANAGER") ||
                    hasRole(auth, "STAFF") && value.equals(auth.getName())
                );
                requireInput(
                    List.of("staff1", "staff2").contains(value),
                    "Choose staff1 or staff2"
                );
                requireInput(
                    !before.equals("RESOLVED"),
                    "Reopen before assigning"
                );

                ticket.assignee = value;
                ticket.status = "IN_PROGRESS";
            }

            case "PRIORITY" -> {
                require(hasRole(auth, "MANAGER"));
                requireInput(
                    !before.equals("RESOLVED"),
                    "Reopen before reprioritizing"
                );
                requireInput(
                    List.of("LOW", "MEDIUM", "HIGH").contains(value),
                    "Invalid priority"
                );

                ticket.priority = value;
                ticket.dueAt = ticket.createdAt.plus(
                    slaHours(value), ChronoUnit.HOURS
                );
            }

            case "ESCALATE" -> {
                require(
                    hasRole(auth, "MANAGER") ||
                    hasRole(auth, "STAFF") &&
                    auth.getName().equals(ticket.assignee)
                );
                requireInput(
                    !before.equals("RESOLVED") && !ticket.escalated,
                    "Only active, non-escalated tickets can be escalated"
                );

                ticket.escalated = true;
            }

            case "DEESCALATE" -> {
                require(hasRole(auth, "MANAGER"));
                requireInput(ticket.escalated, "Ticket is not escalated");

                ticket.escalated = false;
            }

            case "STATUS" -> {
                require(
                    hasRole(auth, "STAFF") || hasRole(auth, "MANAGER")
                );
                require(
                    hasRole(auth, "MANAGER") ||
                    auth.getName().equals(ticket.assignee)
                );
                requireInput(
                    ticket.assignee != null,
                    "Assign ticket before changing status"
                );

                boolean validTransition =
                    before.equals("IN_PROGRESS") &&
                    List.of("WAITING_FOR_STUDENT", "RESOLVED")
                        .contains(value)
                    ||
                    before.equals("WAITING_FOR_STUDENT") &&
                    List.of("IN_PROGRESS", "RESOLVED")
                        .contains(value);

                requireInput(validTransition, "Invalid status transition");

                ticket.status = value;
                ticket.resolvedAt =
                    value.equals("RESOLVED") ? Instant.now() : null;

                if (value.equals("RESOLVED")) {
                    ticket.escalated = false;
                }
            }

            case "REOPEN" -> {
                require(
                    hasRole(auth, "MANAGER") ||
                    hasRole(auth, "STUDENT") &&
                    ticket.studentUsername.equals(auth.getName())
                );
                requireInput(
                    before.equals("RESOLVED"),
                    "Only resolved tickets can be reopened"
                );

                ticket.status = "OPEN";
                ticket.assignee = null;
                ticket.resolvedAt = null;
                ticket.escalated = false;
                ticket.dueAt = Instant.now().plus(
                    24, ChronoUnit.HOURS
                );
            }

            case "COMMENT" -> {
                requireInput(
                    !before.equals("RESOLVED"),
                    "Reopen to add a comment"
                );
                require(
                    !hasRole(auth, "STAFF") ||
                    auth.getName().equals(ticket.assignee)
                );
                requireInput(
                    note != null && !note.isBlank(),
                    "Enter a comment"
                );

                if (hasRole(auth, "STUDENT") &&
                    before.equals("WAITING_FOR_STUDENT")) {
                    ticket.status = "IN_PROGRESS";
                }
            }

            default -> throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Unknown action"
            );
        }

        String detail;

        if (action.equals("COMMENT")) {
            detail = note.trim();
        } else {
            detail = action + " " +
                (value.isEmpty()
                    ? before + " -> " + ticket.status
                    : value) +
                (note == null || note.isBlank()
                    ? ""
                    : "; " + note.trim());
        }

        requireInput(detail.length() <= 2000, "Note too long");

        ticket.updatedAt = Instant.now();

        events.save(new TicketEvent(
            ticket.id, auth.getName(), action, detail
        ));

        return tickets.save(ticket);
    }

    public Map<String, Long> dashboard(Authentication auth) {
        require(
            hasRole(auth, "MANAGER") || hasRole(auth, "STAFF")
        );

        List<Ticket> all = tickets.findAll();
        Instant now = Instant.now();

        return Map.of(
            "total", (long) all.size(),
            "open", all.stream()
                .filter(t -> t.status.equals("OPEN")).count(),
            "active", all.stream()
                .filter(t -> t.status.equals("IN_PROGRESS")).count(),
            "waiting", all.stream()
                .filter(t -> t.status.equals("WAITING_FOR_STUDENT")).count(),
            "resolved", all.stream()
                .filter(t -> t.status.equals("RESOLVED")).count(),
            "escalated", all.stream()
                .filter(t -> t.escalated).count(),
            "overdue", all.stream()
                .filter(t ->
                    !t.status.equals("RESOLVED") &&
                    t.dueAt.isBefore(now)
                ).count()
        );
    }
}