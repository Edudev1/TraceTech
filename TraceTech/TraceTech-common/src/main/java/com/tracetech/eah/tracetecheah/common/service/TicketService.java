package com.tracetech.eah.tracetecheah.common.service;


import com.tracetech.eah.tracetecheah.common.entity.Ticket;
import com.tracetech.eah.tracetecheah.common.entity.TicketStatus;
import com.tracetech.eah.tracetecheah.common.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public List<Ticket> findAll() {
        return ticketRepository.findAll();
    }

    public void deleteById(Long id) {
        ticketRepository.deleteById(id);
    }

    public void archive(Long id) {
        ticketRepository.findById(id).ifPresent(t -> {
            if (t.getStatus() == TicketStatus.CLOSED) {
                t.setArchived(true);
                ticketRepository.save(t);
            }
        });
    }

    public Ticket save(Ticket ticket) {
        return ticketRepository.save(ticket);
    }
    public Optional<Ticket> findById(Long id) {
        return ticketRepository.findById(id);
    }

    public List<Ticket> findAssignedTo(String username) {
        return ticketRepository.findByAssignedToOrderByCreatedAtDesc(username);
    }

    public List<Ticket> findCreatedBy(String username){
        return ticketRepository.findByCreatedByOrderByCreatedAtDesc(username);
    }

    public void assignTo(Long ticketId, String username) {
        ticketRepository.findById(ticketId).ifPresent(ticket -> {
            ticket.setAssignedTo(username == null || username.isBlank() ? null : username);
            ticketRepository.save(ticket);
        });
    }

    public void changeStatus(Long id, TicketStatus status) {
        ticketRepository.findById(id).ifPresent(ticket -> {
            ticket.setStatus(status);
            ticketRepository.save(ticket);
        });
    }
}
