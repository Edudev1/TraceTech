package com.tracetech.eah.tracetecheah.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_activities")
@Getter
@Setter
public class TicketActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TicketActivityType type;

    @Column(nullable = false, length = 80)
    private String actor;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}