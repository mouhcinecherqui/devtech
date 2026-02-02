package devtechly.service;

import devtechly.domain.Ticket;
import devtechly.domain.TicketMessage;
import devtechly.repository.TicketRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service pour l'acceptation et le refus des devis par le client.
 * Toute la logique métier est dans une seule transaction pour éviter
 * l'erreur "Transaction silently rolled back because it has been marked as rollback-only".
 */
@Service
public class TicketDevisService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketDevisService.class);

    private final TicketRepository ticketRepository;
    private final TicketMessageService ticketMessageService;
    private final NotificationService notificationService;

    public TicketDevisService(
        TicketRepository ticketRepository,
        TicketMessageService ticketMessageService,
        NotificationService notificationService
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageService = ticketMessageService;
        this.notificationService = notificationService;
    }

    /**
     * Accepte un devis pour le ticket (appelé après validation créateur + statut "Nouveau").
     */
    @Transactional
    public void acceptDevis(Long ticketId, String login, Map<String, Object> request) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();
        double amountForMessage = extractAmount(request);
        String description = request.get("description") != null ? request.get("description").toString() : "";
        Integer devisIndex = extractDevisIndex(request);

        ticket.setStatus("Devis validé");
        ticket.setPaymentStatus("PENDING");

        String confirmationMessage = String.format(
            "J'accepte le devis #%d d'un montant de %.2f MAD. %s",
            devisIndex != null ? devisIndex + 1 : 1,
            amountForMessage,
            description != null ? description : ""
        );
        ticketMessageService.addMessage(ticketId, confirmationMessage, TicketMessage.AuthorType.CLIENT, login);
        ticketRepository.save(ticket);

        try {
            notificationService.notifyUser("admin", "Le client a accepté le devis pour le ticket #" + ticketId, "DEVIS_ACCEPTED");
        } catch (Exception e) {
            LOG.warn("Notification admin (accept-devis) non envoyée pour le ticket {}: {}", ticketId, e.getMessage());
        }
    }

    /**
     * Refuse un devis pour le ticket (appelé après validation créateur + statut "Nouveau").
     */
    @Transactional
    public void rejectDevis(Long ticketId, String login, Map<String, Object> request) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();
        double amountForMessage = extractAmount(request);
        String description = request.get("description") != null ? request.get("description").toString() : "";
        Integer devisIndex = extractDevisIndex(request);

        String rejectionMessage = String.format(
            "Je refuse le devis #%d d'un montant de %.2f MAD. %s",
            devisIndex != null ? devisIndex + 1 : 1,
            amountForMessage,
            description != null ? description : ""
        );
        ticketMessageService.addMessage(ticketId, rejectionMessage, TicketMessage.AuthorType.CLIENT, login);
        ticketRepository.save(ticket);

        try {
            notificationService.notifyUser("admin", "Le client a refusé le devis pour le ticket #" + ticketId, "DEVIS_REJECTED");
        } catch (Exception e) {
            LOG.warn("Notification admin (reject-devis) non envoyée pour le ticket {}: {}", ticketId, e.getMessage());
        }
    }

    private static double extractAmount(Map<String, Object> request) {
        if (request.get("amount") == null) return 0.0;
        try {
            return Double.parseDouble(request.get("amount").toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static Integer extractDevisIndex(Map<String, Object> request) {
        if (request.get("devisIndex") == null) return null;
        try {
            return Integer.valueOf(request.get("devisIndex").toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
