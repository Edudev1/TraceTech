package com.tracetech.eah.tracetecheah.common.repository;

import com.tracetech.eah.tracetecheah.common.entity.Ticket;
import com.tracetech.eah.tracetecheah.common.entity.TicketActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketActivityRepository extends JpaRepository<TicketActivity, Long> {
    List<TicketActivity> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}