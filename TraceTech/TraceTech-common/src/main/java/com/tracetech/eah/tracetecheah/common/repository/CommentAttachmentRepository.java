package com.tracetech.eah.tracetecheah.common.repository;

import com.tracetech.eah.tracetecheah.common.entity.CommentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentAttachmentRepository extends JpaRepository<CommentAttachment, Long> {
}