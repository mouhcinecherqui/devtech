package devtech.web.rest;

import devtech.domain.Ticket;
import devtech.repository.TicketRepository;
import devtech.security.SecurityUtils;
import devtech.service.MailService;
import devtech.service.NotificationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@Transactional
public class TicketResource {

    private final TicketRepository ticketRepository;
    private final MailService mailService;
    private final NotificationService notificationService;

    public TicketResource(TicketRepository ticketRepository, MailService mailService, NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.mailService = mailService;
        this.notificationService = notificationService;
    }

    // Lister les tickets de l'utilisateur connecté
    @GetMapping("")
    public List<Ticket> getMyTickets() {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return List.of();
        boolean isAdmin = SecurityContextHolder.getContext()
            .getAuthentication()
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_MANAGER"));
        if (isAdmin) {
            return ticketRepository.findAll();
        } else {
            return ticketRepository.findByCreatedBy(login);
        }
    }

    // Créer un ticket
    @PostMapping("")
    public ResponseEntity<Ticket> createTicket(@Valid @RequestBody Ticket ticket) throws URISyntaxException {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return ResponseEntity.badRequest().build();
        ticket.setCreatedBy(login);
        ticket.setCreatedDate(java.time.Instant.now());
        Ticket result = ticketRepository.save(ticket);
        mailService.sendTicketCreatedEmail(result);
        notificationService.notifyUser(login, "Votre ticket a été créé (ID: " + result.getId() + ")", "TICKET_CREATED");
        notificationService.notifyUser("admin", "Nouveau ticket créé par " + login + " (ID: " + result.getId() + ")", "TICKET_CREATED");
        return ResponseEntity.created(new URI("/api/tickets/" + result.getId())).body(result);
    }

    // Mettre à jour le statut et les messages d'un ticket
    @PutMapping("/{id}")
    public ResponseEntity<Ticket> updateTicket(@PathVariable Long id, @Valid @RequestBody Ticket ticket) {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) return ResponseEntity.badRequest().build();
        Ticket existing = ticketRepository.findById(id).orElse(null);
        System.out.println("PUT /api/tickets/" + id + " - trouvé: " + (existing != null) + ", login=" + login);
        if (existing == null) return ResponseEntity.notFound().build();
        boolean isAdmin = SecurityContextHolder.getContext()
            .getAuthentication()
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_MANAGER"));
        if (!isAdmin && !existing.getCreatedBy().equals(login)) {
            return ResponseEntity.status(403).build();
        }
        existing.setStatus(ticket.getStatus());
        existing.setMessages(ticket.getMessages());
        Ticket updated = ticketRepository.save(existing);
        mailService.sendTicketUpdatedEmail(updated);
        notificationService.notifyUser(
            existing.getCreatedBy(),
            "Votre ticket (ID: " + updated.getId() + ") a été mis à jour.",
            "TICKET_UPDATED"
        );
        if (!existing.getCreatedBy().equals(login)) {
            notificationService.notifyUser(login, "Vous avez mis à jour le ticket (ID: " + updated.getId() + ")", "TICKET_UPDATED");
        }
        return ResponseEntity.ok(updated);
    }
}
