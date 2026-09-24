package com.edumerge.support.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class TicketAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public Long ticketId;

    @Column(nullable = false)
    public String filename;

    @Column(nullable = false)
    public String contentType;

    @Column(nullable = false)
    public String uploadedBy;

    @Column(nullable = false)
    public Instant uploadedAt;

    @Lob
    @Column(columnDefinition = "LONGBLOB", nullable = false)
    public byte[] data;
}