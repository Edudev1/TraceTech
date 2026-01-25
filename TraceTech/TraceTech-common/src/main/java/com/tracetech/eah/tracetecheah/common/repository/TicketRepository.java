package com.tracetech.eah.tracetecheah.common.repository;

import com.tracetech.eah.tracetecheah.common.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByAssignedToOrderByCreatedAtDesc(String assignedTo);

    List<Ticket> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    List<Ticket> findByArchivedFalse();

    Optional<Ticket> findByAndArchivedFalse(Long id);
}
