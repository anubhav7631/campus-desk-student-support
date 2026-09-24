package com.edumerge.support.controller;

import jakarta.validation.Valid;
import com.edumerge.support.entity.Ticket;
import com.edumerge.support.entity.TicketEvent;
import com.edumerge.support.repository.TicketRepository;
import com.edumerge.support.repository.EventRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.edumerge.support.entity.Ticket;
import com.edumerge.support.entity.TicketEvent;

@RestController @RequestMapping("/api")
public class TicketController {
    private final TicketRepository tickets;
    private final EventRepository events;
    TicketController(TicketRepository tickets,EventRepository events) { this.tickets=tickets;this.events=events; }
    record CreateTicket(@NotBlank @Size(max=120) String title,@NotBlank @Size(max=2000) String description,
                        @NotBlank String category,@NotBlank String priority) {}
    record Change(@NotBlank String action,String value,String note) {}
    private boolean role(Authentication a,String role) { return a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_"+role)); }
    private void require(boolean allowed) { if(!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not permitted for this role"); }
    private Ticket get(Long id,Authentication a) {
        var t=tickets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ticket not found"));
        if(role(a,"STUDENT")) require(t.studentUsername.equals(a.getName()));
        return t;
    }
    @GetMapping("/me") Map<String,Object> me(Authentication a) {
        return Map.of("username",a.getName(),"role",a.getAuthorities().iterator().next().getAuthority().substring(5));
    }
    @GetMapping("/tickets") List<Ticket> list(Authentication a) {
        return role(a,"STUDENT") ? tickets.findByStudentUsernameOrderByCreatedAtDesc(a.getName()) : tickets.findAllByOrderByCreatedAtDesc();
    }
    @GetMapping("/tickets/{id}") Ticket detail(@PathVariable Long id,Authentication a) { return get(id,a); }
    @GetMapping("/tickets/{id}/events") List<TicketEvent> history(@PathVariable Long id,Authentication a) {
        get(id,a);return events.findByTicketIdOrderByAtAscIdAsc(id);
    }
    @PostMapping("/tickets") @ResponseStatus(HttpStatus.CREATED) @Transactional
    Ticket create(@Valid @RequestBody CreateTicket input,Authentication a) {
        require(role(a,"STUDENT"));
        String category=input.category().trim().toUpperCase(), priority=input.priority().trim().toUpperCase();
        requireInput(List.of("FEES","ATTENDANCE","ID_CARD","DOCUMENTS","OTHER").contains(category),"Invalid category");
        requireInput(List.of("LOW","MEDIUM","HIGH").contains(priority),"Invalid priority");
        var t=new Ticket();t.studentUsername=a.getName();t.title=input.title().trim();t.description=input.description().trim();
        t.category=category;t.priority=priority;t.status="OPEN";t.createdAt=Instant.now();t.updatedAt=t.createdAt;
        t.dueAt=t.createdAt.plus(priority.equals("HIGH")?24:priority.equals("MEDIUM")?48:72,ChronoUnit.HOURS);
        tickets.save(t);events.save(new TicketEvent(t.id,a.getName(),"CREATED","New "+category+" ticket, "+priority+" priority"));return t;
    }
    private void requireInput(boolean ok,String msg) { if(!ok) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,msg); }
    @PatchMapping("/tickets/{id}") @Transactional
    Ticket change(@PathVariable Long id,@Valid @RequestBody Change change,Authentication a) {
        Ticket t=get(id,a);String action=change.action().trim().toUpperCase();String value=change.value()==null?"":change.value().trim();
        String before=t.status;
        switch(action) {
            case "ASSIGN" -> {
                require(role(a,"MANAGER") || role(a,"STAFF") && value.equals(a.getName()));
                requireInput(List.of("staff1","staff2").contains(value),"Choose staff1 or staff2");
                requireInput(!t.status.equals("RESOLVED"),"Reopen before assigning");
                t.assignee=value;t.status="IN_PROGRESS";
            }
            case "PRIORITY" -> {
                require(role(a,"MANAGER"));
                requireInput(!before.equals("RESOLVED"),"Reopen before reprioritizing");
                requireInput(List.of("LOW","MEDIUM","HIGH").contains(value),"Invalid priority");
                t.priority=value;
                t.dueAt=t.createdAt.plus(value.equals("HIGH")?24:value.equals("MEDIUM")?48:72,ChronoUnit.HOURS);
            }
            case "ESCALATE" -> {
                require(role(a,"MANAGER") || role(a,"STAFF") && a.getName().equals(t.assignee));
                requireInput(!before.equals("RESOLVED") && !t.escalated,"Only active, non-escalated tickets can be escalated");
                t.escalated=true;
            }
            case "DEESCALATE" -> {
                require(role(a,"MANAGER"));
                requireInput(t.escalated,"Ticket is not escalated");
                t.escalated=false;
            }
            case "STATUS" -> {
                require(role(a,"STAFF") || role(a,"MANAGER"));
                require(role(a,"MANAGER") || a.getName().equals(t.assignee));
                requireInput(t.assignee!=null,"Assign ticket before changing status");
                requireInput((before.equals("IN_PROGRESS") && List.of("WAITING_FOR_STUDENT","RESOLVED").contains(value))
                   || (before.equals("WAITING_FOR_STUDENT") && List.of("IN_PROGRESS","RESOLVED").contains(value)),"Invalid status transition");
                t.status=value;t.resolvedAt=value.equals("RESOLVED")?Instant.now():null;
                if(value.equals("RESOLVED")) t.escalated=false;
            }
            case "REOPEN" -> {
                require(role(a,"MANAGER") || role(a,"STUDENT") && t.studentUsername.equals(a.getName()));
                requireInput(before.equals("RESOLVED"),"Only resolved tickets can be reopened");
                t.status="OPEN";t.assignee=null;t.resolvedAt=null;t.dueAt=Instant.now().plus(24,ChronoUnit.HOURS);
                t.escalated=false;
            }
            case "COMMENT" -> {
                requireInput(!before.equals("RESOLVED"),"Reopen to add a comment");
                require(!role(a,"STAFF") || a.getName().equals(t.assignee));
                requireInput(change.note()!=null && !change.note().isBlank(),"Enter a comment");
                if(role(a,"STUDENT") && before.equals("WAITING_FOR_STUDENT")) t.status="IN_PROGRESS";
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown action");
        }
        String detail=action.equals("COMMENT")?change.note().trim(): action+" "+(value.isEmpty()?before+" -> "+t.status:value)+
            (change.note()==null || change.note().isBlank()?"":"; "+change.note().trim());
        requireInput(detail.length()<=2000,"Note too long");
        t.updatedAt=Instant.now();events.save(new TicketEvent(t.id,a.getName(),action,detail));return tickets.save(t);
    }
    @GetMapping("/dashboard") Map<String,Long> dashboard(Authentication a) {
        require(role(a,"MANAGER") || role(a,"STAFF"));var all=tickets.findAll();var now=Instant.now();
        return Map.of("total",(long)all.size(),"open",all.stream().filter(t->t.status.equals("OPEN")).count(),
          "active",all.stream().filter(t->t.status.equals("IN_PROGRESS")).count(),
          "waiting",all.stream().filter(t->t.status.equals("WAITING_FOR_STUDENT")).count(),
          "resolved",all.stream().filter(t->t.status.equals("RESOLVED")).count(),
          "escalated",all.stream().filter(t->t.escalated).count(),
          "overdue",all.stream().filter(t->!t.status.equals("RESOLVED") && t.dueAt.isBefore(now)).count());
    }
}
