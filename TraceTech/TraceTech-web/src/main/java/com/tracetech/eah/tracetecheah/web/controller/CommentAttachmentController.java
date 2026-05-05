package com.tracetech.eah.tracetecheah.web.controller;

import com.tracetech.eah.tracetecheah.common.entity.CommentAttachment;
import com.tracetech.eah.tracetecheah.common.repository.CommentAttachmentRepository;
import com.tracetech.eah.tracetecheah.web.service.CommentAttachmentStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.MalformedURLException;
import java.nio.file.Path;

@Controller
public class CommentAttachmentController {

    private final CommentAttachmentRepository attachmentRepository;
    private final CommentAttachmentStorageService storageService;

    public CommentAttachmentController(CommentAttachmentRepository attachmentRepository,
                                       CommentAttachmentStorageService storageService) {
        this.attachmentRepository = attachmentRepository;
        this.storageService = storageService;
    }

    @GetMapping("/attachments/comments/{id}")
    public ResponseEntity<Resource> viewCommentAttachment(@PathVariable Long id,
                                                          Authentication auth) throws MalformedURLException {

        CommentAttachment attachment = attachmentRepository.findById(id).orElseThrow();

        boolean isClient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        String ticketOwner = attachment.getComment().getTicket().getCreatedBy();

        if (isClient && (ticketOwner == null || !ticketOwner.equals(auth.getName()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Path path = storageService.load(attachment);
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getOriginalFilename() + "\"")
                .body(resource);
    }
}