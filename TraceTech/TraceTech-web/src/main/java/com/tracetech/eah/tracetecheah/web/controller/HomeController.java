package com.tracetech.eah.tracetecheah.web.controller;

import com.tracetech.eah.tracetecheah.common.entity.Ticket;
import com.tracetech.eah.tracetecheah.common.entity.TicketStatus;
import com.tracetech.eah.tracetecheah.common.repository.UserRepository;
import com.tracetech.eah.tracetecheah.common.service.TicketService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final TicketService ticketService;
    private final UserRepository userRepository;

    public HomeController(TicketService ticketService, UserRepository userRepository) {
        this.ticketService = ticketService;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String index(Model model, Authentication auth) {
        model.addAttribute("section", "home");

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return "index";
        }

        boolean isClient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        boolean isTech = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TECH"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Ticket> tickets;

        if (isClient) {
            tickets = ticketService.findCreatedBy(auth.getName());
        } else if (isTech) {
            tickets = ticketService.findAssignedTo(auth.getName());
        } else {
            tickets = ticketService.findAll();
        }

        long openCount = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.OPEN)
                .count();

        long progressCount = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.IN_PROGRESS)
                .count();

        long closedCount = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.CLOSED)
                .count();

        long totalCount = tickets.size();

        model.addAttribute("dashboardEnabled", true);
        model.addAttribute("username", auth.getName());
        model.addAttribute("isClient", isClient);
        model.addAttribute("isTech", isTech);
        model.addAttribute("isAdmin", isAdmin);

        model.addAttribute("tickets", tickets.stream().limit(5).toList());
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("openCount", openCount);
        model.addAttribute("progressCount", progressCount);
        model.addAttribute("closedCount", closedCount);

        if (isAdmin) {
            model.addAttribute("userCount", userRepository.count());
        }

        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("section", "about");
        return "about";
    }
}