package com.tracetech.eah.tracetecheah.web.controller;

import com.tracetech.eah.tracetecheah.common.dto.CommentCreateDto;
import com.tracetech.eah.tracetecheah.common.entity.AppUser;
import com.tracetech.eah.tracetecheah.common.entity.Comment;
import com.tracetech.eah.tracetecheah.common.entity.Priority;
import com.tracetech.eah.tracetecheah.common.entity.Role;
import com.tracetech.eah.tracetecheah.common.entity.Ticket;
import com.tracetech.eah.tracetecheah.common.entity.TicketStatus;
import com.tracetech.eah.tracetecheah.common.repository.UserRepository;
import com.tracetech.eah.tracetecheah.common.service.CommentService;
import com.tracetech.eah.tracetecheah.common.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final CommentService commentService;
    private final UserRepository userRepository;

    public TicketController(TicketService ticketService,
                            CommentService commentService,
                            UserRepository userRepository) {
        this.ticketService = ticketService;
        this.commentService = commentService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) TicketStatus status,
                       @RequestParam(required = false) Priority priority,
                       Model model,
                       Authentication auth) {

        boolean isClient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        List<Ticket> tickets = isClient
                ? ticketService.findCreatedBy(auth.getName())
                : ticketService.findAll();

        return renderTicketList(model, tickets, q, status, priority, "tickets", "/tickets");
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("section", "new-ticket");
        model.addAttribute("ticket", new Ticket());
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("priorities", Priority.values());
        return "tickets/create";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("ticket") Ticket ticket,
                         BindingResult bindingResult,
                         Model model,
                         Principal principal,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("section", "new-ticket");
            model.addAttribute("statuses", TicketStatus.values());
            model.addAttribute("priorities", Priority.values());
            return "tickets/create";
        }

        ticket.setCreatedBy(principal.getName());
        ticketService.save(ticket);

        redirectAttributes.addFlashAttribute("successMessage", "Ticket creado correctamente.");
        return "redirect:/tickets";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteTicket(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ticketService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Ticket eliminado correctamente.");
        return "redirect:/tickets";
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ticketService.archive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Ticket archivado correctamente.");
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

                    var assignees = userRepository.findByRole(Role.TECH)
                            .stream()
                            .map(AppUser::getUsername)
                            .toList();

                    model.addAttribute("section", "tickets");
                    model.addAttribute("ticket", ticket);
                    model.addAttribute("comments", commentService.findByTicket(ticket));
                    model.addAttribute("newComment", new CommentCreateDto());
                    model.addAttribute("assignees", assignees);

                    return "tickets/detail";
                })
                .orElse("redirect:/tickets");
    }

    @PostMapping("/{id}/comments")
    public String addComment(@PathVariable Long id,
                             @Valid @ModelAttribute("newComment") CommentCreateDto dto,
                             BindingResult bindingResult,
                             Model model,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {

        var ticketOpt = ticketService.findById(id);
        if (ticketOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "El ticket no existe.");
            return "redirect:/tickets";
        }

        var ticket = ticketOpt.get();

        if (bindingResult.hasErrors()) {
            var assignees = userRepository.findByRole(Role.TECH)
                    .stream()
                    .map(AppUser::getUsername)
                    .toList();

            model.addAttribute("section", "tickets");
            model.addAttribute("ticket", ticket);
            model.addAttribute("comments", commentService.findByTicket(ticket));
            model.addAttribute("newComment", dto);
            model.addAttribute("assignees", assignees);
            return "tickets/detail";
        }

        Comment comment = new Comment();
        comment.setContent(dto.getContent());
        comment.setTicket(ticket);
        comment.setAuthor(principal.getName());

        commentService.save(comment);

        redirectAttributes.addFlashAttribute("successMessage", "Comentario añadido correctamente.");
        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return ticketService.findById(id)
                .map(t -> {
                    model.addAttribute("section", "tickets");
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
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("section", "tickets");
            model.addAttribute("priorities", Priority.values());
            model.addAttribute("statuses", TicketStatus.values());
            return "tickets/edit";
        }

        var opt = ticketService.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "El ticket no existe.");
            return "redirect:/tickets";
        }

        Ticket dbTicket = opt.get();

        dbTicket.setTitle(formTicket.getTitle());
        dbTicket.setDescription(formTicket.getDescription());
        dbTicket.setPriority(formTicket.getPriority());
        dbTicket.setStatus(formTicket.getStatus());

        ticketService.save(dbTicket);

        redirectAttributes.addFlashAttribute("successMessage", "Ticket actualizado correctamente.");
        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam TicketStatus status,
                               RedirectAttributes redirectAttributes) {
        ticketService.changeStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Estado actualizado correctamente.");
        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @PostMapping("/{id}/assign-me")
    public String assignToMe(@PathVariable Long id,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        ticketService.assignTo(id, principal.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Ticket asignado correctamente.");
        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/assign")
    public String assignTicket(@PathVariable Long id,
                               @RequestParam(required = false) String username,
                               RedirectAttributes redirectAttributes) {
        ticketService.assignTo(id, username);
        redirectAttributes.addFlashAttribute("successMessage", "Asignación actualizada correctamente.");
        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @GetMapping("/mine")
    public String myAssignedTickets(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) TicketStatus status,
                                    @RequestParam(required = false) Priority priority,
                                    Model model,
                                    Principal principal) {

        List<Ticket> tickets = ticketService.findAssignedTo(principal.getName());

        return renderTicketList(model, tickets, q, status, priority, "mine", "/tickets/mine");
    }

    @GetMapping("/my-created")
    public String myCreatedTickets(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) TicketStatus status,
                                   @RequestParam(required = false) Priority priority,
                                   Model model,
                                   Principal principal) {

        List<Ticket> tickets = ticketService.findCreatedBy(principal.getName());

        return renderTicketList(model, tickets, q, status, priority, "my-created", "/tickets/my-created");
    }

    private String renderTicketList(Model model,
                                    List<Ticket> tickets,
                                    String q,
                                    TicketStatus status,
                                    Priority priority,
                                    String section,
                                    String listBasePath) {

        if (q != null && !q.isBlank()) {
            String qLower = q.trim().toLowerCase();
            tickets = tickets.stream()
                    .filter(t -> t.getTitle() != null && t.getTitle().toLowerCase().contains(qLower))
                    .toList();
        }

        if (status != null) {
            tickets = tickets.stream()
                    .filter(t -> t.getStatus() == status)
                    .toList();
        }

        if (priority != null) {
            tickets = tickets.stream()
                    .filter(t -> t.getPriority() == priority)
                    .toList();
        }

        model.addAttribute("section", section);
        model.addAttribute("listBasePath", listBasePath);
        model.addAttribute("tickets", tickets);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("priority", priority);
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("priorities", Priority.values());

        return "tickets/list";
    }
}