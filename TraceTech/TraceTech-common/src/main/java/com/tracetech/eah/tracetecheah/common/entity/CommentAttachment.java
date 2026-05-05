package com.tracetech.eah.tracetecheah.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment_attachments")
@Getter
@Setter
public class CommentAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storedFilename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;
}