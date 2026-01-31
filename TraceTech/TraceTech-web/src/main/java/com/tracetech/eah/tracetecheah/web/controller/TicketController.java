package com.tracetech.eah.tracetecheah.web.controller;

import com.tracetech.eah.tracetecheah.common.dto.CommentCreateDto;
import com.tracetech.eah.tracetecheah.common.entity.Comment;
import com.tracetech.eah.tracetecheah.common.entity.Priority;
import com.tracetech.eah.tracetecheah.common.entity.Ticket;
import com.tracetech.eah.tracetecheah.common.entity.TicketStatus;
import com.tracetech.eah.tracetecheah.common.service.CommentService;
import com.tracetech.eah.tracetecheah.common.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final CommentService commentService;

    public TicketController(TicketService ticketService, CommentService commentService) {
        this.ticketService = ticketService;
        this.commentService = commentService;
    }

    @GetMapping
    public String list(Model model, Authentication auth) {

        boolean isClient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        if (isClient) {
            model.addAttribute("tickets", ticketService.findCreatedBy(auth.getName()));
        } else {
            model.addAttribute("tickets", ticketService.findAll());
        }

        return "tickets/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("ticket", new Ticket());
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("priorities", Priority.values());
        return "tickets/create";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("ticket") Ticket ticket,
                         BindingResult bindingResult,
                         Model model,
                         java.security.Principal principal) {

        if (bindingResult.hasErrors()) return "tickets/create";

        ticket.setCreatedBy(principal.getName());
        ticketService.save(ticket);
        return "redirect:/tickets";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteTicket(@PathVariable Long id) {
        ticketService.deleteById(id);
        return "redirect:/tickets";
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id) {
        ticketService.archive(id);
        return "redirect:/tickets";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication auth) {
        return ticketService.findById(id)
                .map(ticket -> {
                    boolean isClient = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

                    if (isClient && (ticket.getCreatedBy() == null || !ticket.getCreatedBy().equals(auth.getName()))) {
                        return "redirect:/tickets";
                    }

                    model.addAttribute("ticket", ticket);
                    model.addAttribute("comments", commentService.findByTicket(ticket));
                    model.addAttribute("newComment", new CommentCreateDto());
                    model.addAttribute("assignees", java.util.List.of("tech"));
                    return "tickets/detail";
                })
                .orElse("redirect:/tickets");
    }

    @PostMapping("/{id}/comments")
    public String addComment(@PathVariable Long id,
                             @Valid @ModelAttribute("newComment") CommentCreateDto dto,
                             BindingResult bindingResult,
                             Model model,
                             Principal principal) {

        var ticketOpt = ticketService.findById(id);
        if (ticketOpt.isEmpty()) return "redirect:/tickets";

        var ticket = ticketOpt.get();

        if (bindingResult.hasErrors()) {
            model.addAttribute("ticket", ticket);
            model.addAttribute("comments", commentService.findByTicket(ticket));
            return "tickets/detail";
        }

        Comment comment = new Comment();
        comment.setContent(dto.getContent());
        comment.setTicket(ticket);
        comment.setAuthor(principal.getName()); // <-- admin/tech/client

        commentService.save(comment);

        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return ticketService.findById(id)
                .map(t -> {
                    model.addAttribute("ticket", t);
                    model.addAttribute("priorities", Priority.values());
                    model.addAttribute("statuses", TicketStatus.values());
                    return "tickets/edit";
                })
                .orElse("redirect:/tickets");
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("ticket") Ticket formTicket,
                         BindingResult bindingResult,
                         Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("priorities", Priority.values());
            model.addAttribute("statuses", TicketStatus.values());
            return "tickets/edit";
        }

        var opt = ticketService.findById(id);
        if (opt.isEmpty()) return "redirect:/tickets";

        Ticket dbTicket = opt.get();

        dbTicket.setTitle(formTicket.getTitle());
        dbTicket.setDescription(formTicket.getDescription());
        dbTicket.setPriority(formTicket.getPriority());
        dbTicket.setStatus(formTicket.getStatus());

        ticketService.save(dbTicket);

        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam TicketStatus status) {
        ticketService.changeStatus(id, status);
        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @PostMapping("/{id}/assign-me")
    public String assignToMe(@PathVariable Long id, Principal principal) {
        ticketService.assignTo(id, principal.getName());
        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/assign")
    public String assignTicket(@PathVariable Long id,
                               @RequestParam(required = false) String username) {
        ticketService.assignTo(id, username);
        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @GetMapping("/mine")
    public String myAssignedTickets(Model model, Principal principal) {
        model.addAttribute("tickets", ticketService.findAssignedTo(principal.getName()));
        return "tickets/list";
    }

    @GetMapping("/my-created")
    public String myCreatedTickets(Model model, Principal principal) {
        model.addAttribute("tickets", ticketService.findCreatedBy(principal.getName()));
        return "tickets/list";
    }

}
