package com.tracetech.eah.tracetecheah.common.service;

import com.tracetech.eah.tracetecheah.common.entity.Ticket;
import com.tracetech.eah.tracetecheah.common.entity.TicketActivity;
import com.tracetech.eah.tracetecheah.common.entity.TicketActivityType;
import com.tracetech.eah.tracetecheah.common.repository.TicketActivityRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketActivityService {

    private final TicketActivityRepository ticketActivityRepository;

    public TicketActivityService(TicketActivityRepository ticketActivityRepository) {
        this.ticketActivityRepository = ticketActivityRepository;
    }

    public List<TicketActivity> findByTicket(Ticket ticket) {
        return ticketActivityRepository.findByTicketOrderByCreatedAtAsc(ticket);
    }

    public void register(Ticket ticket, TicketActivityType type, String actor, String message) {
        TicketActivity activity = new TicketActivity();
        activity.setTicket(ticket);
        activity.setType(type);
        activity.setActor(actor);
        activity.setMessage(message);

        ticketActivityRepository.save(activity);
    }
}