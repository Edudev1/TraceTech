package com.tracetech.eah.tracetecheah.web.controller;


import com.tracetech.eah.tracetecheah.common.entity.TicketStatus;
import com.tracetech.eah.tracetecheah.common.service.TicketService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final TicketService ticketService;

    public HomeController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping({"/"})
    public String index(){
        return "index";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TECH')")
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam TicketStatus status){
        ticketService.changeStatus(id, status);
        return "redirect:/tickets" + id;
    }
}
