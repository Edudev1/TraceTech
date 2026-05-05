package com.tracetech.eah.tracetecheah.web.controller;

import com.tracetech.eah.tracetecheah.common.dto.CommentCreateDto;
import com.tracetech.eah.tracetecheah.common.entity.AppUser;
import com.tracetech.eah.tracetecheah.common.entity.Comment;
import com.tracetech.eah.tracetecheah.common.entity.Priority;
import com.tracetech.eah.tracetecheah.common.entity.Role;
import com.tracetech.eah.tracetecheah.common.entity.Ticket;
import com.tracetech.eah.tracetecheah.common.entity.TicketActivityType;
import com.tracetech.eah.tracetecheah.common.entity.TicketStatus;
import com.tracetech.eah.tracetecheah.common.repository.UserRepository;
import com.tracetech.eah.tracetecheah.common.service.CommentService;
import com.tracetech.eah.tracetecheah.common.service.TicketActivityService;
import com.tracetech.eah.tracetecheah.common.service.TicketService;
import com.tracetech.eah.tracetecheah.web.service.CommentAttachmentStorageService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final CommentService commentService;
    private final UserRepository userRepository;
    private final CommentAttachmentStorageService attachmentStorageService;
    private final TicketActivityService ticketActivityService;

    public TicketController(TicketService ticketService,
                            CommentService commentService,
                            UserRepository userRepository,
                            CommentAttachmentStorageService attachmentStorageService,
                            TicketActivityService ticketActivityService) {
        this.ticketService = ticketService;
        this.commentService = commentService;
        this.userRepository = userRepository;
        this.attachmentStorageService = attachmentStorageService;
        this.ticketActivityService = ticketActivityService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) TicketStatus status,
                       @RequestParam(required = false) Priority priority,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model,
                       Authentication auth) {

        boolean isClient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        List<Ticket> tickets = isClient
                ? ticketService.findCreatedBy(auth.getName())
                : ticketService.findAll();

        return renderTicketList(model, tickets, q, status, priority, page, size, "tickets", "/tickets");
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
        Ticket savedTicket = ticketService.save(ticket);

        ticketActivityService.register(
                savedTicket,
                TicketActivityType.CREATED,
                principal.getName(),
                "Ticket creado"
        );

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
    public String archive(@PathVariable Long id,
                          Principal principal,
                          RedirectAttributes redirectAttributes) {

        var ticketOpt = ticketService.findById(id);
        if (ticketOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "El ticket no existe.");
            return "redirect:/tickets";
        }

        Ticket ticket = ticketOpt.get();

        ticketService.archive(id);

        ticketActivityService.register(
                ticket,
                TicketActivityType.ARCHIVED,
                principal.getName(),
                "Ticket archivado"
        );

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
                    model.addAttribute("activities", ticketActivityService.findByTicket(ticket));
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
                             @RequestParam(required = false) MultipartFile image,
                             Model model,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {

        var ticketOpt = ticketService.findById(id);
        if (ticketOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "El ticket no existe.");
            return "redirect:/tickets";
        }

        Ticket ticket = ticketOpt.get();

        if (bindingResult.hasErrors()) {
            var assignees = userRepository.findByRole(Role.TECH)
                    .stream()
                    .map(AppUser::getUsername)
                    .toList();

            model.addAttribute("section", "tickets");
            model.addAttribute("ticket", ticket);
            model.addAttribute("comments", commentService.findByTicket(ticket));
            model.addAttribute("activities", ticketActivityService.findByTicket(ticket));
            model.addAttribute("newComment", dto);
            model.addAttribute("assignees", assignees);
            return "tickets/detail";
        }

        Comment comment = new Comment();
        comment.setContent(dto.getContent());
        comment.setTicket(ticket);
        comment.setAuthor(principal.getName());

        Comment savedComment = commentService.save(comment);

        try {
            if (image != null && !image.isEmpty()) {
                attachmentStorageService.store(image, savedComment);
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/tickets/" + id;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo subir la imagen.");
            return "redirect:/tickets/" + id;
        }

        ticketActivityService.register(
                ticket,
                TicketActivityType.COMMENTED,
                principal.getName(),
                "Comentario añadido"
        );

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
                         Principal principal,
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

        ticketActivityService.register(
                dbTicket,
                TicketActivityType.UPDATED,
                principal.getName(),
                "Ticket actualizado"
        );

        redirectAttributes.addFlashAttribute("successMessage", "Ticket actualizado correctamente.");
        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam TicketStatus status,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {

        var ticketOpt = ticketService.findById(id);
        if (ticketOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "El ticket no existe.");
            return "redirect:/tickets";
        }

        Ticket ticket = ticketOpt.get();
        TicketStatus oldStatus = ticket.getStatus();

        ticketService.changeStatus(id, status);

        ticketActivityService.register(
                ticket,
                TicketActivityType.STATUS_CHANGED,
                principal.getName(),
                "Estado cambiado de " + statusLabel(oldStatus) + " a " + statusLabel(status)
        );

        redirectAttributes.addFlashAttribute("successMessage", "Estado actualizado correctamente.");
        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @PostMapping("/{id}/assign-me")
    public String assignToMe(@PathVariable Long id,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {

        var ticketOpt = ticketService.findById(id);
        if (ticketOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "El ticket no existe.");
            return "redirect:/tickets";
        }

        Ticket ticket = ticketOpt.get();

        ticketService.assignTo(id, principal.getName());

        ticketActivityService.register(
                ticket,
                TicketActivityType.ASSIGNED,
                principal.getName(),
                "Ticket asignado a " + principal.getName()
        );

        redirectAttributes.addFlashAttribute("successMessage", "Ticket asignado correctamente.");
        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/assign")
    public String assignTicket(@PathVariable Long id,
                               @RequestParam(required = false) String username,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {

        var ticketOpt = ticketService.findById(id);
        if (ticketOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "El ticket no existe.");
            return "redirect:/tickets";
        }

        Ticket ticket = ticketOpt.get();

        ticketService.assignTo(id, username);

        String assignedText = (username == null || username.isBlank())
                ? "Ticket dejado sin asignar"
                : "Ticket asignado a " + username;

        ticketActivityService.register(
                ticket,
                TicketActivityType.ASSIGNED,
                principal.getName(),
                assignedText
        );

        redirectAttributes.addFlashAttribute("successMessage", "Asignación actualizada correctamente.");
        return "redirect:/tickets/" + id;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @GetMapping("/mine")
    public String myAssignedTickets(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) TicketStatus status,
                                    @RequestParam(required = false) Priority priority,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    Model model,
                                    Principal principal) {

        List<Ticket> tickets = ticketService.findAssignedTo(principal.getName());

        return renderTicketList(model, tickets, q, status, priority, page, size, "mine", "/tickets/mine");
    }

    @GetMapping("/my-created")
    public String myCreatedTickets(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) TicketStatus status,
                                   @RequestParam(required = false) Priority priority,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model,
                                   Principal principal) {

        List<Ticket> tickets = ticketService.findCreatedBy(principal.getName());

        return renderTicketList(model, tickets, q, status, priority, page, size, "my-created", "/tickets/my-created");
    }

    @GetMapping("/export/csv")
    public void exportTicketsCsv(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) TicketStatus status,
                                 @RequestParam(required = false) Priority priority,
                                 Authentication auth,
                                 HttpServletResponse response) throws IOException {

        boolean isClient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        List<Ticket> tickets = isClient
                ? ticketService.findCreatedBy(auth.getName())
                : ticketService.findAll();

        tickets = applyTicketFilters(tickets, q, status, priority);

        writeTicketsCsv(response, tickets, "tickets.csv");
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @GetMapping("/mine/export/csv")
    public void exportMyAssignedTicketsCsv(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) TicketStatus status,
                                           @RequestParam(required = false) Priority priority,
                                           Principal principal,
                                           HttpServletResponse response) throws IOException {

        List<Ticket> tickets = ticketService.findAssignedTo(principal.getName());
        tickets = applyTicketFilters(tickets, q, status, priority);

        writeTicketsCsv(response, tickets, "mis-asignados.csv");
    }

    @GetMapping("/my-created/export/csv")
    public void exportMyCreatedTicketsCsv(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) TicketStatus status,
                                          @RequestParam(required = false) Priority priority,
                                          Principal principal,
                                          HttpServletResponse response) throws IOException {

        List<Ticket> tickets = ticketService.findCreatedBy(principal.getName());
        tickets = applyTicketFilters(tickets, q, status, priority);

        writeTicketsCsv(response, tickets, "mis-creados.csv");
    }

    @GetMapping("/export/excel")
    public void exportTicketsExcel(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) TicketStatus status,
                                   @RequestParam(required = false) Priority priority,
                                   Authentication auth,
                                   HttpServletResponse response) throws IOException {

        boolean isClient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        List<Ticket> tickets = isClient
                ? ticketService.findCreatedBy(auth.getName())
                : ticketService.findAll();

        tickets = applyTicketFilters(tickets, q, status, priority);

        writeTicketsExcel(response, tickets, "tickets.xlsx");
    }

    @PreAuthorize("hasAnyRole('ADMIN','TECH')")
    @GetMapping("/mine/export/excel")
    public void exportMyAssignedTicketsExcel(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) TicketStatus status,
                                             @RequestParam(required = false) Priority priority,
                                             Principal principal,
                                             HttpServletResponse response) throws IOException {

        List<Ticket> tickets = ticketService.findAssignedTo(principal.getName());
        tickets = applyTicketFilters(tickets, q, status, priority);

        writeTicketsExcel(response, tickets, "mis-asignados.xlsx");
    }

    @GetMapping("/my-created/export/excel")
    public void exportMyCreatedTicketsExcel(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) TicketStatus status,
                                            @RequestParam(required = false) Priority priority,
                                            Principal principal,
                                            HttpServletResponse response) throws IOException {

        List<Ticket> tickets = ticketService.findCreatedBy(principal.getName());
        tickets = applyTicketFilters(tickets, q, status, priority);

        writeTicketsExcel(response, tickets, "mis-creados.xlsx");
    }

    private String renderTicketList(Model model,
                                    List<Ticket> tickets,
                                    String q,
                                    TicketStatus status,
                                    Priority priority,
                                    int page,
                                    int size,
                                    String section,
                                    String listBasePath) {

        tickets = applyTicketFilters(tickets, q, status, priority);

        int totalItems = tickets.size();

        if (size <= 0) {
            size = 10;
        }

        int totalPages = (int) Math.ceil((double) totalItems / size);

        if (totalPages == 0) {
            totalPages = 1;
        }

        if (page < 0) {
            page = 0;
        }

        if (page >= totalPages) {
            page = totalPages - 1;
        }

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalItems);

        List<Ticket> pagedTickets = totalItems == 0
                ? List.of()
                : tickets.subList(fromIndex, toIndex);

        int showingFrom = totalItems == 0 ? 0 : fromIndex + 1;
        int showingTo = toIndex;

        model.addAttribute("section", section);
        model.addAttribute("listBasePath", listBasePath);
        model.addAttribute("csvExportPath", listBasePath + "/export/csv");
        model.addAttribute("excelExportPath", listBasePath + "/export/excel");

        model.addAttribute("tickets", pagedTickets);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("showingFrom", showingFrom);
        model.addAttribute("showingTo", showingTo);

        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hasPreviousPage", page > 0);
        model.addAttribute("hasNextPage", page < totalPages - 1);

        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("priority", priority);
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("priorities", Priority.values());

        return "tickets/list";
    }

    private List<Ticket> applyTicketFilters(List<Ticket> tickets,
                                            String q,
                                            TicketStatus status,
                                            Priority priority) {

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

        return tickets;
    }

    private void writeTicketsCsv(HttpServletResponse response,
                                 List<Ticket> tickets,
                                 String filename) throws IOException {

        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);

        var writer = response.getWriter();

        writer.println("ID,Título,Estado,Prioridad,Creado por,Asignado a,Creado,Archivado");

        for (Ticket t : tickets) {
            writer.println(
                    csv(t.getId()) + "," +
                            csv(t.getTitle()) + "," +
                            csv(statusLabel(t.getStatus())) + "," +
                            csv(priorityLabel(t.getPriority())) + "," +
                            csv(t.getCreatedBy()) + "," +
                            csv(t.getAssignedTo()) + "," +
                            csv(t.getCreatedAt()) + "," +
                            csv(t.isArchived() ? "Sí" : "No")
            );
        }

        writer.flush();
    }

    private void writeTicketsExcel(HttpServletResponse response,
                                   List<Ticket> tickets,
                                   String filename) throws IOException {

        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tickets");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);

            String[] columns = {
                    "ID",
                    "Título",
                    "Estado",
                    "Prioridad",
                    "Creado por",
                    "Asignado a",
                    "Creado",
                    "Archivado"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;

            for (Ticket t : tickets) {
                Row row = sheet.createRow(rowIndex++);

                row.createCell(0).setCellValue(t.getId() != null ? t.getId() : 0);
                row.createCell(1).setCellValue(nullSafe(t.getTitle()));
                row.createCell(2).setCellValue(statusLabel(t.getStatus()));
                row.createCell(3).setCellValue(priorityLabel(t.getPriority()));
                row.createCell(4).setCellValue(nullSafe(t.getCreatedBy()));
                row.createCell(5).setCellValue(nullSafe(t.getAssignedTo()));
                row.createCell(6).setCellValue(t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
                row.createCell(7).setCellValue(t.isArchived() ? "Sí" : "No");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }

        String text = value.toString().replace("\"", "\"\"");

        return "\"" + text + "\"";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String statusLabel(TicketStatus status) {
        if (status == null) {
            return "";
        }

        return switch (status) {
            case OPEN -> "Abierto";
            case IN_PROGRESS -> "En progreso";
            case CLOSED -> "Cerrado";
        };
    }

    private String priorityLabel(Priority priority) {
        if (priority == null) {
            return "";
        }

        return switch (priority) {
            case LOW -> "Baja";
            case MEDIUM -> "Media";
            case HIGH -> "Alta";
        };
    }
}