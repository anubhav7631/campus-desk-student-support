package com.edumerge.support.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(
    name = "tickets",
    indexes = {
        @Index(name = "idx_ticket_student", columnList = "student_username"),
        @Index(name = "idx_ticket_assignee", columnList = "assignee")
    }
)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "student_username", nullable = false, length = 80)
    public String studentUsername;

    @Column(nullable = false, length = 120)
    public String title;

    @Column(nullable = false, length = 2000)
    public String description;

    @Column(nullable = false, length = 30)
    public String category;

    @Column(nullable = false, length = 20)
    public String priority;

    @Column(nullable = false, length = 30)
    public String status;

    @Column(length = 80)
    public String assignee;

    @Column(nullable = false)
    public boolean escalated = false;

    @Column(nullable = false)
    public Instant createdAt;

    @Column(nullable = false)
    public Instant updatedAt;

    @Column(nullable = false)
    public Instant dueAt;

    public Instant resolvedAt;

    @Version
    public Long version;
}