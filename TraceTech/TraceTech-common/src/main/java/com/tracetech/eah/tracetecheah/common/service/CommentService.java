package com.tracetech.eah.tracetecheah.common.service;


import com.tracetech.eah.tracetecheah.common.entity.Comment;
import com.tracetech.eah.tracetecheah.common.entity.Ticket;
import com.tracetech.eah.tracetecheah.common.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }
    public List<Comment> findByTicket(Ticket ticket) {
        return commentRepository.findByTicketOrderByCreatedAtAsc(ticket);
    }
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }
}
