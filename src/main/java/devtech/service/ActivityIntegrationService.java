package devtechly.service;

import devtechly.domain.ActivityType;
import devtechly.domain.Ticket;
import devtechly.domain.User;
import devtechly.repository.UserRepository;
import devtechly.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for automatically creating activities based on system events.
 */
@Service
@Transactional
public class ActivityIntegrationService {

    private final Logger log = LoggerFactory.getLogger(ActivityIntegrationService.class);

    private final ActivityService activityService;
    private final UserRepository userRepository;

    public ActivityIntegrationService(ActivityService activityService, UserRepository userRepository) {
        this.activityService = activityService;
        this.userRepository = userRepository;
    }

    /**
     * Create activity when a ticket is created.
     */
    public void createTicketCreatedActivity(Ticket ticket) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }

        try {
            Long userId = getCurrentUserId();
            String title = String.format("Nouveau ticket créé #%d", ticket.getId());
            String description = String.format(
                "Ticket de type '%s': %s",
                ticket.getType() != null ? ticket.getType() : "Non spécifié",
                ticket.getDescription() != null ? ticket.getDescription() : "Aucune description"
            );

            activityService.createActivity(ActivityType.INFO, title, description, userId, ticket.getId(), "TICKET", ticket.getId());

            log.debug("Created ticket created activity for ticket #{}", ticket.getId());
        } catch (Exception e) {
            log.error("Error creating ticket created activity for ticket #{}: {}", ticket.getId(), e.getMessage());
        }
    }

    /**
     * Create activity when a ticket status is updated.
     */
    public void createTicketStatusUpdatedActivity(Ticket ticket, String oldStatus, String newStatus) {
        if (ticket == null || ticket.getId() == null || oldStatus == null || newStatus == null) {
            return;
        }

        if (oldStatus.equals(newStatus)) {
            return; // No change
        }

        try {
            Long userId = getCurrentUserId();
            ActivityType activityType = getActivityTypeForStatus(newStatus);
            String title = String.format("Ticket #%d %s", ticket.getId(), getStatusActionText(newStatus));
            String description = String.format("Statut changé de '%s' vers '%s'", oldStatus, newStatus);

            activityService.createActivity(activityType, title, description, userId, ticket.getId(), "TICKET", ticket.getId());

            log.debug("Created ticket status updated activity for ticket #{}: {} -> {}", ticket.getId(), oldStatus, newStatus);
        } catch (Exception e) {
            log.error("Error creating ticket status updated activity for ticket #{}: {}", ticket.getId(), e.getMessage());
        }
    }

    /**
     * Create activity when a payment is processed.
     */
    public void createPaymentActivity(Long ticketId, String paymentType, String amount, boolean success) {
        if (ticketId == null) {
            return;
        }

        try {
            Long userId = getCurrentUserId();
            ActivityType activityType = success ? ActivityType.SUCCESS : ActivityType.ERROR;
            String title = String.format("Paiement %s pour le ticket #%d", success ? "validé" : "échoué", ticketId);
            String description = String.format(
                "%s de %s pour le ticket #%d",
                success ? "Paiement validé" : "Échec du paiement",
                amount != null ? amount : "montant inconnu",
                ticketId
            );

            activityService.createActivity(activityType, title, description, userId, ticketId, "PAYMENT", ticketId);

            log.debug("Created payment activity for ticket #{}: {} - {}", ticketId, paymentType, success ? "success" : "failed");
        } catch (Exception e) {
            log.error("Error creating payment activity for ticket #{}: {}", ticketId, e.getMessage());
        }
    }

    /**
     * Create activity when a quote is generated.
     */
    public void createQuoteActivity(Long ticketId, String amount) {
        if (ticketId == null) {
            return;
        }

        try {
            Long userId = getCurrentUserId();
            String title = String.format("Devis généré pour le ticket #%d", ticketId);
            String description = String.format(
                "Devis de %s généré pour le ticket #%d",
                amount != null ? amount : "montant à définir",
                ticketId
            );

            activityService.createActivity(ActivityType.INFO, title, description, userId, ticketId, "QUOTE", ticketId);

            log.debug("Created quote activity for ticket #{}: {}", ticketId, amount);
        } catch (Exception e) {
            log.error("Error creating quote activity for ticket #{}: {}", ticketId, e.getMessage());
        }
    }

    /**
     * Create activity for system events.
     */
    public void createSystemActivity(ActivityType type, String title, String description) {
        try {
            Long userId = getCurrentUserId();

            activityService.createActivity(type, title, description, userId, null, "SYSTEM", 1L);

            log.debug("Created system activity: {}", title);
        } catch (Exception e) {
            log.error("Error creating system activity: {}", e.getMessage());
        }
    }

    /**
     * Get current user ID from security context.
     */
    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId().orElse(null);
    }

    /**
     * Get activity type based on ticket status.
     */
    private ActivityType getActivityTypeForStatus(String status) {
        if (status == null) {
            return ActivityType.INFO;
        }

        return switch (status.toLowerCase()) {
            case "résolu", "fermé", "paiement validé" -> ActivityType.SUCCESS;
            case "urgent" -> ActivityType.WARNING;
            case "erreur", "annulé" -> ActivityType.ERROR;
            default -> ActivityType.INFO;
        };
    }

    /**
     * Get action text for status change.
     */
    private String getStatusActionText(String status) {
        if (status == null) {
            return "mis à jour";
        }

        return switch (status.toLowerCase()) {
            case "nouveau" -> "créé";
            case "en cours" -> "mis en cours";
            case "résolu" -> "résolu";
            case "fermé" -> "fermé";
            case "urgent" -> "marqué urgent";
            case "en attente de paiement" -> "mis en attente de paiement";
            case "paiement validé" -> "paiement validé";
            case "erreur" -> "marqué en erreur";
            case "annulé" -> "annulé";
            default -> "mis à jour";
        };
    }
}
