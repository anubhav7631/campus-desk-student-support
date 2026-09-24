package com.edumerge.support.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.edumerge.support.entity.Ticket;

@Service
public class ManagerEmailService {

    private final JavaMailSender sender;
    private final String from;
    private final String managerEmail;

    public ManagerEmailService(
            JavaMailSender sender,
            @Value("${spring.mail.username}") String from,
            @Value("${app.mail.manager}") String managerEmail) {

        this.sender = sender;
        this.from = from;
        this.managerEmail = managerEmail;
    }

    public void sendEscalation(Ticket ticket) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(managerEmail);
        message.setSubject(
            "Campus Desk: ticket #" + ticket.id + " escalated"
        );
        message.setText(
            "An overdue ticket requires review.\n\n" +
            "Ticket ID: " + ticket.id + "\n" +
            "Title: " + ticket.title + "\n" +
            "Priority: " + ticket.priority + "\n" +
            "Status: " + ticket.status + "\n" +
            "Assigned to: " +
                (ticket.assignee == null ? "Unassigned" : ticket.assignee) +
                "\nDue at: " + ticket.dueAt
        );

        sender.send(message);
    }
}