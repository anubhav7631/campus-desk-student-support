package com.edumerge.support.controller;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.edumerge.support.entity.Ticket;
import com.edumerge.support.entity.TicketAttachment;
import com.edumerge.support.repository.AttachmentRepository;
import com.edumerge.support.repository.TicketRepository;

@RestController
@RequestMapping("/api/tickets/{ticketId}/attachments")
public class AttachmentController {

    private final TicketRepository tickets;
    private final AttachmentRepository attachments;

    public AttachmentController(
            TicketRepository tickets,
            AttachmentRepository attachments) {
        this.tickets = tickets;
        this.attachments = attachments;
    }

    private Ticket authorizedTicket(Long ticketId, Authentication auth) {
        Ticket ticket = tickets.findById(ticketId).orElseThrow(
            () -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Ticket not found"
            )
        );

        boolean isStudent = auth.getAuthorities().stream()
            .anyMatch(authority ->
                authority.getAuthority().equals("ROLE_STUDENT")
            );

        // A student may access only their own ticket.
        // Staff and managers may access tickets in the shared queue.
        if (isStudent &&
            !ticket.studentUsername.equals(auth.getName())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You cannot access this ticket"
            );
        }

        return ticket;
    }

    @GetMapping
    public List<Map<String, Object>> list(
            @PathVariable Long ticketId,
            Authentication auth) {

        authorizedTicket(ticketId, auth);

        return attachments
            .findByTicketIdOrderByUploadedAtDesc(ticketId)
            .stream()
            .map(attachment -> Map.<String, Object>of(
                "id", attachment.id,
                "filename", attachment.filename,
                "uploadedBy", attachment.uploadedBy,
                "uploadedAt", attachment.uploadedAt
            ))
            .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> upload(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file,
            Authentication auth) throws IOException {

        Ticket ticket = authorizedTicket(ticketId, auth);

        if ("RESOLVED".equals(ticket.status)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Reopen the ticket before uploading a file"
            );
        }

        if (file.isEmpty() || file.getSize() > 2_000_000) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Choose a file up to 2 MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
            !List.of(
                "application/pdf",
                "image/png",
                "image/jpeg"
            ).contains(contentType)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only PDF, PNG and JPEG files are allowed"
            );
        }

        TicketAttachment attachment = new TicketAttachment();
        attachment.ticketId = ticketId;

        String originalName = file.getOriginalFilename();
        attachment.filename = originalName == null
            ? "attachment"
            : originalName.replaceAll("[^a-zA-Z0-9._-]", "_");

        attachment.contentType = contentType;
        attachment.uploadedBy = auth.getName();
        attachment.uploadedAt = Instant.now();
        attachment.data = file.getBytes();

        attachments.save(attachment);

        return Map.of(
            "id", attachment.id,
            "filename", attachment.filename
        );
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<byte[]> download(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId,
            Authentication auth) {

        authorizedTicket(ticketId, auth);

        TicketAttachment attachment =
            attachments.findById(attachmentId).orElseThrow(
                () -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Attachment not found"
                )
            );

        if (!attachment.ticketId.equals(ticketId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Attachment not found for this ticket"
            );
        }

        String disposition = ContentDisposition.attachment()
            .filename(attachment.filename)
            .build()
            .toString();

        return ResponseEntity.ok()
            .contentType(
                MediaType.parseMediaType(attachment.contentType)
            )
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
            .body(attachment.data);
    }
}