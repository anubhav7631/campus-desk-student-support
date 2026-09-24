package com.edumerge.support.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edumerge.support.entity.TicketAttachment;

public interface AttachmentRepository
        extends JpaRepository<TicketAttachment, Long> {

    List<TicketAttachment> findByTicketIdOrderByUploadedAtDesc(
        Long ticketId
    );
}