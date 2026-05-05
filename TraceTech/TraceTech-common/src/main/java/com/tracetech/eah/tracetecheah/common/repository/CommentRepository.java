package com.tracetech.eah.tracetecheah.common.repository;

import com.tracetech.eah.tracetecheah.common.entity.Comment;
import com.tracetech.eah.tracetecheah.common.entity.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "attachments")
    List<Comment> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}